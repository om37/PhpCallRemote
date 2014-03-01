package om37.phpcall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.text.format.Time;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A starting point for my nfc project.
 * Contains code experimenting with reading from and writing to tags.
 * 
 * Lots of help from android developer site
 * @author Odie
 *
 */

public class MainActivity extends Activity {

	NfcAdapter mAdapter;//NFC adapter
	NdefMessage messageToWrite;//Message that will be written
	
	//To handle pending intents/newly scanned tags
	PendingIntent  pending;
	IntentFilter[] intentFiltersArray;
	String[][] 	   techListArray;
	
	Ndef theTag;
	
	Handler 	   handler;//Handler for runnable
	EditText 	   usernameBox;//The username box
	EditText 	   roomNumberBox;//The roomnumber box
	String 		   time;//The time
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); 
		
		mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
		
		setupPendingActivity();
		
		handler = new Handler();
		usernameBox = (EditText)findViewById(R.id.userNameEntry);
		roomNumberBox = (EditText)findViewById(R.id.roomNumber);
		
		if(checkIntent())//If onCreate called by NDEF discovery
			run();//Do something
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
//		mAdapter.disableForegroundDispatch(this);//Disable foreground dispatch when lose focus
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		mAdapter.enableForegroundDispatch(this, pending, intentFiltersArray, techListArray);
	}
	
	@Override
	public void onNewIntent(Intent intent)//Called if a tag is rescanned
	{
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		theTag = Ndef.get(tag);
		
		Toast.makeText(getApplicationContext(), "New tag detected" , Toast.LENGTH_SHORT).show();
		TextView tv = (TextView)findViewById(R.id.display);
		
		NdefMessage[] messages = getMessages(intent);
		if(messages==null)
		{
			Toast.makeText(getApplicationContext(), "Empty Tag", Toast.LENGTH_SHORT).show();
			tv.setText("Empty tag scanned");
			return;
		}
		
		int mes=0;
		int rec=0;
		String s="";
		for(NdefMessage m:messages)
		{
			mes++;
			for(NdefRecord r:m.getRecords())
			{
				rec++;
				s =  "On new intent: \n\r";
				s += "Mes "+mes+" Rec "+rec+" Pay ";
				s+= new String(r.getPayload());
				s+= "\n\r";
			}
		}
		tv.setText(s);
	}

	/*
	 * Instantiates a pending activity to allow foreground dispatch to be enabled.
	 * Sets up the necessary intent and tech filters to specify the intents we're interested in intercepting.
	 * 
	 * ********************
	 * TO DO:
	 * Add filter for AAR
	 * ********************
	 * 
	 */
	public void setupPendingActivity() {
		pending = PendingIntent.getActivity(
			this, 
			0, 
			new Intent(this,getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
			0
		);		
		IntentFilter ndefPlainTextFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);		
		try
		{
			ndefPlainTextFilter.addDataType("text/plain");
		}
		catch(MalformedMimeTypeException e)
		{
		}
		intentFiltersArray = new IntentFilter[]{ndefPlainTextFilter,};
		techListArray = new String[][]{new String[]{Ndef.class.getName()}};
	}
	
	public boolean checkIntent()
	{		
		return NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()) ? true : false;
	}
	
	public void run()
	{
		Ndef tag = getNdefFromIntent(getIntent());
		theTag = tag;
		NdefMessage[] messages = getMessages(getIntent());
		try
		{
			NdefMessage message = tag.getNdefMessage();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		int mes=0;
		int rec=0;
		
		String s="";
		for(NdefMessage m:messages)
		{
			mes++;
			for(NdefRecord r:m.getRecords())
			{
				rec++;
				s =  "onCreate --> Run: \n\rMes "+mes+" Rec "+rec+" Pay ";
				s+= new String(r.getPayload());
				s+= "\n\r";
			}
		}
		TextView tv = (TextView)findViewById(R.id.display);
		tv.setText(s);
	}

	
	public void testFunc()
	{
		for(int i = 0; i < 2; i++)
		{
			Toast.makeText(getApplicationContext(), "HelloWorld", Toast.LENGTH_LONG).show();
		}
	}
	
	public void writeButton(View view)
	{
		createMessageToWrite();
	}
	public void createMessageToWrite()
	{		
		NdefRecord textRecord = NdefRecord.createMime("text/plain", "This is some text from Odie's writer".getBytes());
		NdefRecord aar 		  = NdefRecord.createApplicationRecord(getPackageName());
		
		NdefMessage message = new NdefMessage(new NdefRecord[]{textRecord, aar});
		messageToWrite=message;
		Toast.makeText(getApplicationContext(), "Message made", Toast.LENGTH_SHORT).show();
		
		write();
	}
	
	public void write()
	{		
		Toast.makeText(getApplicationContext(),"Starting write. Touch tag",Toast.LENGTH_SHORT).show();
		sleep(3000);
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				Toast.makeText(getApplicationContext(),"Starting runnable. Touch tag",Toast.LENGTH_SHORT).show();
				sleep(3000);
				
				if(messageToWrite == null)
				{
					Toast.makeText(getApplicationContext(),"Message is null",Toast.LENGTH_SHORT).show();
					return;
				}
				
				Toast.makeText(getApplicationContext(), "Trying", Toast.LENGTH_SHORT).show();
				sleep(3000);
				//Some test comment for github
				try
				{					
					if(theTag != null)
					{theTag.connect();
					theTag.writeNdefMessage(messageToWrite);
					theTag.close();}
					else{
						Toast.makeText(getApplicationContext(), "tag is null", Toast.LENGTH_SHORT).show();
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				catch (FormatException e)
				{
					e.printStackTrace();
				}
				Toast.makeText(getApplicationContext(),"Written",Toast.LENGTH_SHORT).show();
			}
		};
		Thread t = new Thread(r);
		t.start();
	}
	
	public Ndef getNdefFromIntent(Intent intent)
	{
		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Ndef ndefTag = Ndef.get(tag);
		
		Toast t = Toast.makeText(getApplicationContext(),
					"Ndef tag scanned. NDEF object created. Tag type: " + ndefTag.getType(),
					Toast.LENGTH_LONG);
		t.show();
		
		return ndefTag;
	}
	
	public NdefMessage[] getMessages(Intent intent)
	{
		Parcelable[] msgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		
		NdefMessage[] ndefMsgs=null;
		if(msgs != null)
		{
			ndefMsgs = new NdefMessage[msgs.length];
            for (int i = 0; i < msgs.length; i++)
            {
                ndefMsgs[i] = (NdefMessage) msgs[i];
            }
		}
		return ndefMsgs;
	}
	
	public void sleep(int time)
	{
		try{Thread.sleep(time);}catch(Exception e){}
	}
	//http://stackoverflow.com/questions/18425942/android-send-post-data-to-server
	public void SendPost(View view)
	{		
		Time t = new Time();
		t.setToNow();
		time = t.toString();
		
		Runnable r = new Runnable()
		{	
			HttpClient httpClient = new DefaultHttpClient();//Client to execute the post
			HttpPost post = new HttpPost("http://www.om37nfcregistration.net46.net/loginWithPost.php/");//The post object/event
			List<NameValuePair> postData = new ArrayList<NameValuePair>();
			
			//Create the post Vars
			String username = usernameBox.getText().toString();
			BasicNameValuePair id = new BasicNameValuePair("studentId", username);//studentId
		
			String roomNumber = roomNumberBox.getText().toString();
			BasicNameValuePair rm = new BasicNameValuePair("roomNum", roomNumber);//roomNum			
			
			BasicNameValuePair tm = new BasicNameValuePair("dateTime", time);//timeDate
			
			@Override
			public void run() 
			{
				//Add post vars
				postData.add(id);
				postData.add(rm);
				postData.add(tm);
				
				try 
				{
					post.setEntity(new UrlEncodedFormEntity(postData));
					httpClient.execute(post);
				}				
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}		
		};
		
		Thread thread = new Thread(r);
		thread.start();
	}

}
