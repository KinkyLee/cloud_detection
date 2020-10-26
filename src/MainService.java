import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;


public class MainService implements Runnable {

	static boolean notUseIns = false;

	static final int APP_ID = 0x0f;
	private static final int SQL_STATEMENT = 1;
	private static final int EXTENDED_INS = 0x20;
	private static final int MAC_INS_TYPE = 0x02;
	private static final int PRELIMINARY_SHUTDOWN = 0x08;
	private static final int CLEAR_DATA = 0x07;
	StateOfApp stateOfApp;
	public static DatabaseHelper sDatabaseHelper;
	public static String sStorageLocation;
	SqlManager manager;

	CloudDetectionUtils cloudDetectionUtils;
	
	class QueryParam {
		String tableName;
		String projection;
		String selection;
		String[] selectionArgs;
		String sortOrder;
	}

	public void report(String r) {
		System.out.println(r);
	}
	
	private void clearData(SqlManager manager) {
		DatabaseHelper.clearTable(manager);
		stateOfApp.setClearData();
	}

	public void onCreate() {
		sStorageLocation = System.getProperty("user.dir");

		manager = new SqlManager();
		if (manager != null) {
			manager.connectDB();
		} else {
			report("ContentObserver: Cannot Create SqlManager!");
		}

		stateOfApp = new StateOfApp();
		stateOfApp.setLaunchState();

	}
	
	public synchronized void run() {
		while(true) {
			String sqlString = "select zl_lx,zl_bh,zl_nr,zl_id from zl where zl_yyid = 15 and zl_zxjg=0";
			ResultSet rs = manager.executeQuery(sqlString);
			
			try {
				while (rs.next()) {
					report("A command is executing");
					
					long curInsID = rs.getLong(4);
					int curInsType = rs.getInt(1);
					int curInsNo = rs.getInt(2);
					String curInsContent = rs.getString(3);

					if (!((curInsType == MAC_INS_TYPE) && (curInsNo == PRELIMINARY_SHUTDOWN))) {
						try {
							sqlString = String.format("update zl set zl_zxjg=1 where zl_id=%d", curInsID);
							if (manager.executeUpdate(sqlString) <= 0) {
								report("ContentObserver: Update ins state failed!");
							}
						} catch (NullPointerException ignored) {
						}
	                }
					
					switch(curInsType) {					
					case MAC_INS_TYPE:
                    	if (curInsNo == CLEAR_DATA) {
							DatabaseHelper.clearTable(manager);
							clearData(manager);
						}
						break;
					case EXTENDED_INS:
					{
						switch (curInsNo) {
						case SQL_STATEMENT:{
							QueryParam queryParam = getQueryParam(curInsContent);
							try {
								String sql = "select " + queryParam.projection + " from "  + queryParam.tableName + " where " + queryParam.selection;
								ResultSet rset = manager.executeQuery(sql);
								
								while (rset != null && rset.next()) {
									int picID = rset.getInt(1);
									int picTaskID = rset.getInt(2);
									String fileName = rset.getString(3);
									String pathName = rset.getString(4);
									int width = rset.getInt(5);
									int height = rset.getInt(6);
									String cloudRatio = rset.getString(7);
									String imgAvalibility = rset.getString(8);
									
									if((fileName != null) && (pathName != null)) {
										report("File name:" + sStorageLocation + File.separator + pathName + File.separator + fileName);
                                        
                                        report("Image name:" + fileName);
                                            cloudDetectionUtils = new CloudDetectionUtils(width, height);
                                            if(cloudDetectionUtils != null) {
                                                try {
//													cloudDetectionUtils.thres(pathName + File.separator + fileName);
                                                	cloudDetectionUtils.thres(sStorageLocation + File.separator + pathName + File.separator + fileName);
												} catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
                                            }
                                            else {
                                                stateOfApp.setErrReport("ERROR");
                                                return;
                                            }

//                                            double cloudRatio = CloudDetectionUtils.getCloudRatio();
//                                            double imgAvalibility = CloudDetectionUtils.getImgAvalibility();
                                            
                                            String sql2 = "insert into " + DatabaseHelper.YJC.TABLE_NAME 
                                            		+ "(" 
                                            		+ DatabaseHelper.YJC.TXID + ","
                                            		+ DatabaseHelper.YJC.TX_RW_ID + ","
                                            		+ DatabaseHelper.YJC.YFGL + ","
                                            		+ DatabaseHelper.YJC.TXKYD  + ","
                                            		+ DatabaseHelper.YJC.MC + ","
                                            		+ DatabaseHelper.YJC.LJ + ","
                                            		+ ")values("
                                            		+ picID + ","
                                            		+ picTaskID + ",'"
                                            		+ cloudRatio + "','"
                                            		+ imgAvalibility + "','"
                                            		+ fileName + "','"
                                            		+ pathName + "')";
                                            
                                            
//                                            manager.executeUpdate(sql2);

                                            stateOfApp.setResult(picID, cloudRatio, imgAvalibility);
                                            report("Cloud coverage:" + String.valueOf(cloudRatio));
                                            report("Image availability:" + String.valueOf(imgAvalibility));
									}
									else {
                                        String content = "Can not get fileName or pathName!";
                                        stateOfApp.setErrReport(content);
                                    }
								}
								
							} catch (NullPointerException e) {
                                String content = "CameraService is not running!";
                                stateOfApp.setErrReport(content);
                                return;
                            }
						}
						break;
						default:
							break;
						}//end switch (curInsNo)
					}
						break;
						default:
							break;
					} //end switch(curInsType)
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private QueryParam getQueryParam(String sql) {
		
		QueryParam queryParam = new QueryParam();
		String[] strs = sql.split(";");
		
		String[] uri = strs[0].split("/");
		queryParam.tableName = uri[uri.length-1];

		if (strs[1].equalsIgnoreCase("null")) {
			queryParam.projection = null;
		} else {
			queryParam.projection = strs[1];
		}
		if (strs[2].equalsIgnoreCase("null")) {
			queryParam.selection = null;
		} else {
			queryParam.selection = strs[2];

		}
		if (strs[3].equalsIgnoreCase("null")) {
			queryParam.selectionArgs = null;
		} else {
			queryParam.selectionArgs = strs[3].split(",");
		}
		if (strs[4].equalsIgnoreCase("null")) {
			queryParam.sortOrder = null;
		} else {
			queryParam.sortOrder = strs[4];
		}
		return queryParam;
	}

	public String byteToString(byte[] bytes) {
		try {
			String sql = new String(bytes, "ASCII");
			return sql.replaceAll("\u0000", "");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static void main(String[] args) {
		
		if(args.length  == 0) {	
			SqlManager.DBhost = "localhost";
			SqlManager.DBport = "3306";
			SqlManager.DBuser = "root";
			SqlManager.DBpasswd = "root";
			SqlManager.DBname = "tz1";	
		}
			
		else {
				SqlManager.DBhost = args[0];
				SqlManager.DBport = args[1];
				SqlManager.DBuser = args[2];
				SqlManager.DBpasswd = args[3];
				SqlManager.DBname = args[4];	
		}
		System.out.println("host: " + SqlManager.DBhost);
		System.out.println("port: " + SqlManager.DBport);
		System.out.println("user: " + SqlManager.DBuser);
		System.out.println("password: " + SqlManager.DBpasswd);
		System.out.println("database: " + SqlManager.DBname);

		MainService mainService = new MainService();
		mainService.onCreate();
		new Thread(mainService).start();
	}
}
