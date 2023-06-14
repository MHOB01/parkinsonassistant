package com.example.parkinsonassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> noteList;

    public NoteAdapter(List<Note> noteList) {
        this.noteList = noteList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single note item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        // Bind the note data to the views in the item layout
        Note note = noteList.get(position);
        holder.textViewNoteText.setText(note.getNoteText());
        holder.textViewTimestamp.setText(formatTimestamp(note.getTimestamp()));

        // Set the font size for the note
        holder.textViewNoteText.setTextSize(28); // Choose the desired font size
        holder.textViewTimestamp.setTextSize(28);
    }

    // Method to format the timestamp
    private String formatTimestamp(Date timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(timestamp);
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewNoteText;
        public TextView textViewTimestamp;

        public NoteViewHolder(View itemView) {
            super(itemView);
            textViewNoteText = itemView.findViewById(R.id.textViewNoteText);
            textViewTimestamp = itemView.findViewById(R.id.textViewTimestamp);
        }
    }
}


