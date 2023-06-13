package com.example.parkinsonassistant;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private Context context;
    private List<String> notesList;

    public NotesAdapter(List<String> notesList) {
        this.context = context;
        this.notesList = notesList;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        String note = notesList.get(position);
        holder.textViewNote.setText(note);
    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textViewNote;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNote = itemView.findViewById(R.id.textViewNote);
        }
    }
}

