class DatabaseHelper  {

    interface YJC {
        String TABLE_NAME = "yjc";
        String ID = "id";
        String TXID= "txid";
        String TX_RW_ID = "tx_rw_id";
        String LJ = "tx_lj";
        String MC = "tx_mc";
        String YFGL = "yfgl";
        String TXKYD = "txkyd";
    }

    public static void clearTable(SqlManager manager) {
        String[] sqls = new String[]{
                "delete form " + YJC.TABLE_NAME
        };

        for(String sql : sqls) {
        	manager.executeUpdate(sql);
        }
    }
}
