package com.sourcey.linachatbot;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by amrezzat on 3/4/2017.
 */

public class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {

    public ChatMessageAdapter(Activity context, List<ChatMessage> chatMessages) {
        super(context, 0, chatMessages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ChatMessage chatMessage = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.chat_message_layout, parent, false);
        }
        LinearLayout message= (LinearLayout) convertView.findViewById(R.id.message);
        TextView chatText = (TextView) convertView.findViewById(R.id.message_text);
        chatText.setText(chatMessage.getMessageText());
        if(chatMessage.isHumanUser()) {
            chatText.setBackground(convertView.getResources().getDrawable(R.drawable.user_rounded_corner));
            message.setGravity(Gravity.RIGHT);
        }
        else {
            chatText.setBackground(convertView.getResources().getDrawable(R.drawable.bot_rounded_corner));
            message.setGravity(Gravity.LEFT);
        }
        return convertView;
    }
}
