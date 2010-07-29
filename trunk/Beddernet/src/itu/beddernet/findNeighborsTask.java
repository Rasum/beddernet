package itu.beddernet;

import itu.beddernet.approuter.IBeddernetService;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.util.Log;

public class findNeighborsTask extends AsyncTask<Object, Object, Object>{
	IBeddernetService service;
	private String TAG = itu.beddernet.common.BeddernetInfo.TAG;
	
	public findNeighborsTask(IBeddernetService service){
		this.service = service;
	}

	@Override
	protected Object doInBackground(Object... params) {
		try {
			service.findNeighbors();
		} catch (RemoteException e) {
			Log.e(TAG, "Could not request device discovery");
		}
		return null;
	}
//	 protected void onPostExecute(Long result) {
////service.discoveryFinished();
//     }
	
	

}
