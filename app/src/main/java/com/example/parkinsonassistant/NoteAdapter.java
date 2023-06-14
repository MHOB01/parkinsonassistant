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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        holder.textViewNoteText.setText(note.getNoteText());
        //holder.textViewContent.setText(note.getContent());
        holder.textViewTimestamp.setText(formatTimestamp(note.getTimestamp()));

        // Setze die Schriftgröße für die Notiz
        holder.textViewNoteText.setTextSize(28); // Wähle die gewünschte Schriftgröße
        holder.textViewTimestamp.setTextSize(28);
    }


    // Methode zum Formatieren des Zeitstempels
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

