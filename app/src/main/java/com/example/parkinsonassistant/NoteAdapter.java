package com.example.parkinsonassistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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

        // Setze die Schriftgröße für die Notiz
        holder.textViewNoteText.setTextSize(28); // Wähle die gewünschte Schriftgröße
    }


    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textViewNoteText;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNoteText = itemView.findViewById(R.id.textViewNoteText);
        }
    }
}

