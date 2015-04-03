package net.hodags.destinycompanionappapitest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends Activity implements OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.my_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        Button b = (Button)findViewById(R.id.my_button);
        b.setClickable(false);
        new LongRunningGetIO().execute();
    }

    private class LongRunningGetIO extends AsyncTask <Void, Void, String> {

        protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
            InputStream in = entity.getContent();
            StringBuffer out = new StringBuffer();
            int n = 1;
            while (n>0) {
                byte[] b = new byte[4096];
                n =  in.read(b);
                if (n>0) out.append(new String(b, 0, n));
            }
            return out.toString();
        }

        @Override
        protected String doInBackground(Void... params) {

            //Steps:
            // (1) Search username to get membershipType & membershipId
            // (2) Use membershipType & membershipId to get characterId(s)
            // (3) Use characterId(s) to get inventory (etc.)


            // Step 1
            String url1 = "http://www.bungie.net/platform/Destiny/SearchDestinyPlayer/1/";
            EditText etusername = (EditText) findViewById(R.id.username);
            String username = etusername.getText().toString();
            url1 += username;
            //adding trailing slash to URI so it doesn't fail
            url1 += "/";

            String response1 = getFromRestUrl(url1);

            String membershipId = null;
            String membershipType = null;
            String displayName = null;

            //parse out membershipType, membershipId and displayName (for later use)
            try {
                JSONObject json = new JSONObject(response1);
                JSONArray responsearray = json.getJSONArray("Response");
                JSONObject responseobj = responsearray.getJSONObject(0);
                membershipType = responseobj.getString("membershipType");
                membershipId = responseobj.getString("membershipId");
                displayName = responseobj.getString("displayName");
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Step 2

            //URL to get details from the membershipId & membershipType
            String url2 = null;
            url2 = "http://www.bungie.net/platform/Destiny/";
            url2 += membershipType;
            url2 += "/Account/";
            url2 += membershipId;
            url2 += "/";

            String response2 = getFromRestUrl(url2);

            //Parse out characterId
            String characterId = null;
            try {
                JSONObject json = new JSONObject(response2);
                JSONObject responseobj = json.getJSONObject("Response");
                JSONObject dataobj = responseobj.getJSONObject("data");
                JSONArray characterters = dataobj.getJSONArray("characters");
                JSONObject characterbases = characterters.getJSONObject(0);
                JSONObject characterbase = characterbases.getJSONObject("characterBase");
                characterId = characterbase.getString("characterId");
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Step 3

            //URL to get inventory (equipped only for now, need to login for full inventorY)
            // {membershipType}/Account/{destinyMembershipId}/Character/{characterId}/Inventory/
            String url3 = null;
            url3 = "http://www.bungie.net/platform/Destiny/";
            url3 += membershipType;
            url3 += "/Account/";
            url3 += membershipId;
            url3 += "/Character/";
            url3 += characterId;
            url3 += "/Inventory/";

            String response3 = getFromRestUrl(url3);

// TODO: parse out inventory
/*
            //Parse out...?
            String characterId = null;
            try {
                JSONObject json = new JSONObject(response3);
                JSONObject responseobj = json.getJSONObject("Response");
                JSONObject dataobj = responseobj.getJSONObject("data");
                JSONArray characterters = dataobj.getJSONArray("characters");
                JSONObject characterbases = characterters.getJSONObject(0);
                JSONObject characterbase = characterbases.getJSONObject("characterBase");

                characterId = characterbase.getString("characterId");
            } catch (Exception e) {
                e.printStackTrace();
            }

*/

            //temporary response of characterId (from step 2)
            //String finalReturn = characterId;

            //temporary response of full inventory (equipped, since not logged in yet)
            String finalReturn = response3;

            return finalReturn;
        }

        protected void onPostExecute(String results) {
            if (results!=null) {
                EditText et = (EditText)findViewById(R.id.my_edit);
                et.setText(results);
            }
            Button b = (Button)findViewById(R.id.my_button);
            b.setClickable(true);
        }

        protected String getFromRestUrl(String inboundUrl) {
            String returnValue = null;

            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            List<NameValuePair> headers = new ArrayList<NameValuePair>();
            //add headers here
            headers.add(new BasicNameValuePair("X-API-Key:", "ed499fbe9b994102b961c0b00e15048f"));  //HODAGS' API KEY
            String url = inboundUrl;

            HttpGet httpGet = new HttpGet(url);
            for (NameValuePair h : headers)
            {
                httpGet.addHeader(h.getName(), h.getValue());
            }

            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                returnValue = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
                return e.getLocalizedMessage();
            }

            return returnValue;
        }
    }
}