package org.hfoss.posit.android.bluetooth;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class SelectFindListAdapter extends BaseAdapter {

	private Context mContext;

	private List<SelectFind> mItems = new ArrayList<SelectFind>();

	public SelectFindListAdapter(Context context) {
		mContext = context;
	}

	public void addItem(SelectFind item) {
		mItems.add(item);
	}

	public void setListItems(List<SelectFind> loItems) {
		mItems = loItems;
	}

	public int getCount() {
		return mItems.size();
	}

	public Object getItem(int position) {
		return mItems.get(position);
	}

	public void setState(boolean state, int position) {
		mItems.get(position).setState(state);
		// Redraw
		this.notifyDataSetChanged();
	}

	public void toggleState(int position) {
		mItems.get(position).toggleState();
		// Redraw
		this.notifyDataSetChanged();
	}

	public void selectAll() {
		for (SelectFind item : mItems) {
			item.setState(true);
		}
		// Redraw
		this.notifyDataSetChanged();
	}

	public void deselectAll() {
		for (SelectFind item : mItems) {
			item.setState(false);
		}
		// Redraw
		this.notifyDataSetChanged();
	}

	public long getItemId(int position) {
		// Item position is its id
		return position;
	}
	
	/**
	 * Get the guids of all the finds selected to be synced
	 * @return String array of the guids
	 */
	public String[] getSelectedGuids() {	
		ArrayList<String> guids = new ArrayList<String>();
		
		for (SelectFind find : mItems) {
			if (find.getState()) guids.add(find.getGuid());
		}

		return guids.toArray(new String[0]);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		/*
		 * TODO: Commented this section out since reusing a view caused really
		 * weird behaviour from the checkboxes, like selecting one would select
		 * all of them.
		 */
		 
		// Right now, just generate a new view on a (re)draw
		return new SelectFindView(mContext, mItems.get(position));
	}
}
