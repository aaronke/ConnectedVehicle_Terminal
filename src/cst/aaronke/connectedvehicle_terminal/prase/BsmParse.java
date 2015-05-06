package cst.aaronke.connectedvehicle_terminal.prase;

import cst.aaronke.connectedvehicle_terminal.utilities.MsgObject;

public class BsmParse {

	static public MsgObject bsm_parseMsg(String msgString){
		MsgObject msgObject=new MsgObject();
		String bsmtemp;
		if (msgString!=null) {
			
			bsmtemp=msgString.substring(7, 8);
			msgObject.setBsm_count(bsmtemp);
			
			bsmtemp=msgString.substring(8, 12);
			msgObject.setBsm_tembID(bsmtemp);
			
			bsmtemp=msgString.substring(14, 18);
			msgObject.setBsm_latitude(bsmtemp);
			
			bsmtemp=msgString.substring(18, 22);
			msgObject.setBsm_longitude(bsmtemp);
			
			bsmtemp=msgString.substring(22, 24);
			msgObject.setBsm_elevation(bsmtemp);
			
			bsmtemp=msgString.substring(28, 32);
			msgObject.setBsm_speed(bsmtemp);
			
			
		}
		
		return msgObject;
		
	}
}
