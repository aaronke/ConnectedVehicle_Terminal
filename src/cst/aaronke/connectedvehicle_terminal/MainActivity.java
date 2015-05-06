package cst.aaronke.connectedvehicle_terminal;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import cst.aaronke.connectedvehicle_terminal.prase.BsmParse;
import cst.aaronke.connectedvehicle_terminal.utilities.MsgObject;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends FragmentActivity implements OnMapLoadedCallback{
	GoogleMap googleMap;
	private TextToSpeech textToSpeech;
	private BluetoothAdapter bluetoothAdapter;
	private final static int DISCURABLE_TIME=300;
	private final static int REQUEST_CODE_BT=1;
	private final static UUID MY_UUID=UUID.fromString("66841278-c3d1-11df-ab31-001de000a903");
	private final static String MY_NAME="Connect_Vehicle_Terminal";
	private AcceptThread acceptThread;
	private ConnectedThread mConnectedThread;
	private int message_flag=1;
	private final static int MESSAGE_CONNECTED=2;
	private final static int MESSAGE_READ=3;
	private final static int MESSAGE_WRITE=4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        googleMap=((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
     
        final LatLng edmontonLatLng=new LatLng(53.52, -113.52);
        googleMap.setMyLocationEnabled(true);
       
        UiSettings uiSettings=googleMap.getUiSettings();
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(edmontonLatLng, 15));
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			
			@Override
			public void onInit(int status) {
				// TODO Auto-generated method stub
				if (status==TextToSpeech.SUCCESS) {
					speakToText("Welcome to CST Connected Vehicle Lab");
				}
			}
		});
        
        // setup BlueTooth
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter==null) {
			Toast.makeText(getApplicationContext(), "You Device Does not Supoort BlueTooth!", Toast.LENGTH_SHORT).show();
		}
       if (bluetoothAdapter.getScanMode()!=BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
		Intent discoverInent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverInent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCURABLE_TIME);
		startActivityForResult(discoverInent, REQUEST_CODE_BT);
       }else {
		
       }
    }
    
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_BT:
			if (resultCode==DISCURABLE_TIME) {
				if (acceptThread==null) {
					acceptThread=new AcceptThread();
					acceptThread.start();
				}
			}
			break;

		default:
			break;
		}
	}
    
    private class AcceptThread extends Thread{
    	private BluetoothServerSocket mServerSocket=null;
    	public AcceptThread(){
    		BluetoothServerSocket tmp=null;
    		try {
				tmp=bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(MY_NAME, MY_UUID);
				Log.v("CVTerminal", "serverSocket is creatred");
			} catch (Exception e) {
				// TODO: handle exception
				Log.v("CVTerminal", "serverSocket fail to create");
			}
    		mServerSocket=tmp;
    	}
    	public void run(){
    		BluetoothSocket mBluetoothSocket=null;
    		while (true) {
				try {
					mBluetoothSocket=mServerSocket.accept();
				} catch (Exception e) {
					// TODO: handle exception
					Log.v("CVTerminal", "Unalbe to connect");
					break;
				}
				if (mBluetoothSocket!=null) {
					Connected(mBluetoothSocket);
					Log.v("CVTerminal", "bluetooth connected");
					try {
						mServerSocket.close();
					} catch (Exception e) {
						// TODO: handle exception
					}
					break;
				}
			}
    	}
    	public void cancel(){
    		try {
				mServerSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
    	}
    	
    }
     
    public synchronized void Connected(BluetoothSocket socket){
    	if (acceptThread!=null) {
			acceptThread.cancel(); acceptThread=null;
		}
    	if (mConnectedThread!=null) {
			mConnectedThread.cancel(); mConnectedThread=null;
		}
    	mConnectedThread=new ConnectedThread(socket);
    	mConnectedThread.start();
    }
    
    private class ConnectedThread extends Thread{
    	private final BluetoothSocket mSocket;
    	private final InputStream mInputStream;
    	private final OutputStream mOutputStream;
    	
    	public ConnectedThread(BluetoothSocket socket){
    		mSocket=socket;
    		InputStream tmpInputStream=null;
    		OutputStream tmpOutputStream=null;
    		try {
				tmpInputStream=socket.getInputStream();
				tmpOutputStream=socket.getOutputStream();
			} catch (Exception e) {
				// TODO: handle exception
				Log.v("CVTerminal", "tmp socket fail to create");
			}
    		
    		mInputStream=tmpInputStream;
    		mOutputStream=tmpOutputStream;
    	}
    	
    	public void run(){
    		byte[] buffer=new byte[1024];
    		int bytes;
    		String messageString="Happy Wife, Happy Life!";
    		byte[] sent_message=messageString.getBytes();
    		while (true) {
				try {
				//	if (message_flag==1) {
						bytes=mInputStream.read(buffer);
						mHandler.obtainMessage(MESSAGE_CONNECTED).sendToTarget();
						mHandler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
				//	}
				//	else {
				//		mOutputStream.write(sent_message);
				//		mHandler.obtainMessage(MESSAGE_WRITE).sendToTarget();
				//	}				
				} catch (Exception e) {
					// TODO: handle exception
					Log.v("CVTerminal", "Disconnected");
				}
			}
    	}
    	
    	public void cancel(){
    		try {
				mSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
				Log.v("CVTerminal", "fail to close Socket");
			}
    	}
    
    }
    
    private static Handler mHandler=new Handler(){
    	public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_READ:
				byte[] readbuf=(byte[])msg.obj;
			//	String read_message=new String(readbuf,0,msg.arg1);
				int size= readbuf.length;
				int dataint[]= new int[size];
				StringBuilder bStringBuilder=new StringBuilder();
				for (int i = 0; i < msg.arg1; i++) {
					dataint[i]=readbuf[i];
					bStringBuilder.append(Integer.toHexString(dataint[i]));
				}
				String message_getString=bStringBuilder.toString();
				
				MsgObject msgObject=BsmParse.bsm_parseMsg(message_getString);
				Log.v("BSM", "count:"+msgObject.getBsm_count()+"--tempID:"+msgObject.getBsm_tembID()+"--latitude:"+msgObject.getBsm_latitude()+"--longitude:"+msgObject.getBsm_longitude()+"--eveluation:"+msgObject.getBsm_elevation()+"--speed:"+msgObject.getBsm_speed());
				Log.v("CVTerminal", "Read:"+message_getString);
				break;
			case MESSAGE_WRITE:
				Log.v("CVTerminal", "write to the Server");
				break;
			case MESSAGE_CONNECTED:
				Log.v("CVTerminal", "BlueTooth Connected");
				break;
			default:
				break;
			}
		}
    };
  
    public void speakToText(String textString){
    	textToSpeech.speak(textString, TextToSpeech.QUEUE_FLUSH, null);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
        	
        	try {
				demoShow();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void demoShow() throws InterruptedException{
    	final Double[] latlist= {
    			53.46224925,
    			53.4625503,
    			53.46273539,
    			53.46284107,
    			53.46296394,
    			53.46309629,
    			53.46324424,
    			53.46336405,
    			53.46352492,
    			53.4636679,
    			53.46379929,
    			53.46394922,
    			53.46409052,
    			53.46419931,
    			53.46434278,
    			53.4644398,
    			53.46457071,
    			53.4646735,
    			53.46478476,
    			53.46492564,
    			53.46504608,
    			53.46517329,
    			53.46531888,
    			53.46545513,
    			53.46560141,
    			53.46572724,
    			53.46587287,
    			53.46602983,
    			53.46615338,
    			53.46629213};
    	final Double[] lonlist={
    			-113.5162428,
    			-113.5162143,
    			-113.5162143,
    			-113.5162348,
    			-113.5162456,
    			-113.5162385,
    			-113.5161988,
    			-113.5161759,
    			-113.5161585,
    			-113.5161487,
    			-113.516125,
    			-113.5161172,
    			-113.5160919,
    			-113.516088,
    			-113.5160678,
    			-113.5160622,
    			-113.5160504,
    			-113.5160552,
    			-113.5160624,
    			-113.5160582,
    			-113.5160545,
    			-113.5160467,
    			-113.5160464,
    			-113.5160412,
    			-113.5160481,
    			-113.5160426,
    			-113.5160376,
    			-113.5160485,
    			-113.5160657,
    			-113.5160738};

    		 OnCameraChangeListener onCameraChangeListener=new OnCameraChangeListener() {
    			 int i=0;
    			 @Override
				public void onCameraChange(CameraPosition arg0) {
					// TODO Auto-generated method stub
					if (i<lonlist.length) {
						LatLng tmpLatLng=new LatLng(latlist[i], lonlist[i]);
						googleMap.clear();
						MarkerOptions markerOptions=new MarkerOptions().position(tmpLatLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.dot)).title("Here am I").snippet("Speed: "+(100+new Random().nextInt(15))+"km/h");
	    				
						Marker myselMarker=	googleMap.addMarker(markerOptions);
						myselMarker.showInfoWindow();
	    		    	if (i>5) {
	    		    		LatLng tmpLatLng2=new LatLng(latlist[i-5], lonlist[i-5]);
							MarkerOptions markerOptions2=new MarkerOptions().position(tmpLatLng2).icon(BitmapDescriptorFactory.fromResource(R.drawable.dot2)).title("Vehicle #12345").snippet("Speed: 100km/h");
						Marker Vehicle_12345=googleMap.addMarker(markerOptions2);
							//Vehicle_12345.showInfoWindow();
						}
	    		    	googleMap.animateCamera(CameraUpdateFactory.newLatLng(tmpLatLng));
	    		    	i++;
					}else {
						i=0;
					}
    				
				}
			};
			 googleMap.setOnCameraChangeListener(onCameraChangeListener);
    }
	@Override
	public void onMapLoaded() {
		// TODO Auto-generated method stub
		
	}
}
