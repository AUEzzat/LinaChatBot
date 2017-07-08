package com.sourcey.linachatbot;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

/**
 * Created by amrezzat on 7/7/2017.
 */

public class ShowDialog extends DialogFragment {


    private OnFragmentClickListener mListener;
    private Activity activity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.activity = activity;
            mListener = (OnFragmentClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement listeners!");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] list = new String[]{};
        if (getArguments().getStringArrayList("list") != null) {
            list = getArguments().getStringArrayList("list").toArray(new String[]{});
        }
        final DefaultHashMap<String, String> data = (DefaultHashMap) getArguments().getSerializable("data");
        final int carryId = getArguments().getInt("carry_id");
        if (carryId == 10) {
            builder.setView(R.layout.edit_real_time);
        }
        builder.setTitle(getArguments().getString("title"))
                .setMessage(getArguments().getString("message"))
                .setItems(list, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onFragmentClick(which + carryId, data);
                    }
                })
                .setPositiveButton(getArguments().getString("redB"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (5 + carryId == 15) {
                            data.put("message", ((TextView) getDialog().findViewById(R.id.new_message)).getText().toString());
                        }
                        mListener.onFragmentClick(5 + carryId, data);
                    }
                })

                .setNeutralButton(getArguments().getString("grayB"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onFragmentClick(6 + carryId, null);
                    }
                })
                .setNegativeButton(getArguments().getString("greenB"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onFragmentClick(7 + carryId, null);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
