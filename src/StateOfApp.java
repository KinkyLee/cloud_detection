import java.text.SimpleDateFormat;
import java.util.Date;

class StateOfApp {
	
	SqlManager manager = null;
	int YYID = 0x0f;
	
	public StateOfApp() {
		manager = new SqlManager();	
		if(manager != null) {
			manager.connectDB();
		}
		else {
			report("ContentObserver: Cannot Create SqlManager!");
		}
	}
	
	public void report(String r) {
		System.out.println(r);
	}

	int sendRealTimeTelemetry(String content) {  
        String sql = String.format("insert into ssyc set ssyc_yyid=%d, ssyc_nr='%s'", YYID, content);
		report("StateOfApp: " + content);
		return manager.executeUpdate(sql);
    }

    void setResult(int picID, String cloudRatio, String imgAvalibility) {
        String content = "PicID:" + picID + ", CloudRatio:" + cloudRatio
                + " ImgAvalibility:" + imgAvalibility;
        sendRealTimeTelemetry(content);
    }

    void setErrReport(String content) {
        sendRealTimeTelemetry(content);
    }

    void setLaunchState() {
    	SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");
        Date date = new Date();
        
        sendRealTimeTelemetry("AppStarted:"+String.valueOf(MainService.APP_ID) + "  Run-up Time:" + sdf.format(date));
    }

    void setClearData() {
        sendRealTimeTelemetry("Clear Data Suc!");
    }
}
