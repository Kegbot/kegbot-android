package org.kegbot.kegtap;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.proto.Api.TapDetail;
import org.kegbot.proto.Api.TapDetailSet;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TapListFragment extends ListFragment {

  private ArrayAdapter<TapDetail> mAdapter;

  private KegbotApi mApi;
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new ArrayAdapter<TapDetail>(getActivity(), R.layout.tap_list_item, R.id.title) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View view =  super.getView(position, convertView, parent);
        TapDetail tap = getItem(position);
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
        title.setText(getTitle(tap));
        subtitle.setText("Left: " + tap.getKeg().getPercentFull());
        return view;
      }
      
    };
    setListAdapter(mAdapter);
  }
  
  void setKegbotApi(KegbotApi api) {
    mApi = api;
  }
  
  void loadTaps() {
    new TapLoaderTask().execute();
  }
  
  private Spanned getTitle(TapDetail tap) {
    String tapName = tap.getTap().getName();
    String beverageName = tap.hasBeverage() ? tap.getBeverage().getName() : getResources().getString(R.string.empty_tap);
    return Html.fromHtml(getResources().getString(R.string.tap_name, tapName, beverageName));
  }
  
  private class TapLoaderTask extends AsyncTask<Void, Void, TapDetailSet> {

    @Override
    protected TapDetailSet doInBackground(Void... params) {
      try {
        return mApi.getAllTaps();
      } catch (KegbotApiException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override
    protected void onPreExecute() {
      setListShown(false);
    }

    @Override
    protected void onPostExecute(TapDetailSet result) {
      mAdapter.clear();
      if (result != null) {        
        mAdapter.addAll(result.getTapsList());
        setListShown(true);
      }
    }    
  }
}
