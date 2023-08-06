package com.example.parkinsonassistant;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class ViewPagerAdapter extends PagerAdapter {

    Context context;

    int images[] = {

            R.drawable.image1,
            R.drawable.startseitebtn,
            R.drawable.menubtn,
            R.drawable.sprachsteuerungbtn

    };

    int headings[] = {

            R.string.heading_one,
            R.string.heading_two,
            R.string.heading_three,
            R.string.heading_fourth
    };

    int description[] = {

            R.string.desc_one,
            R.string.desc_two,
            R.string.desc_three,
            R.string.desc_fourth
    };

    public ViewPagerAdapter(Context context){

        this.context = context;

    }

    @Override
    public int getCount() {
        return  headings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }


    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slider_layout, container, false);

        ImageView slidetitleimage = view.findViewById(R.id.titleImage);
        TextView slideHeading = view.findViewById(R.id.texttitle);
        TextView slideDesciption = view.findViewById(R.id.textdeccription);

        slidetitleimage.setImageResource(images[position]);
        slideDesciption.setText(description[position]);

        // Erhalte die Schriftgröße basierend auf der Bildschirmgröße
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        if (scaledDensity > 2) { // Tablets mit größerer Schriftgröße
            slideHeading.setTextSize(context.getResources().getDimension(R.dimen.heading_size_small_tablet) / scaledDensity);
            slideDesciption.setTextSize(context.getResources().getDimension(R.dimen.text_size_small_tablet) / scaledDensity);

        } else { // Tablets mit kleinerer Schriftgröße
            slideHeading.setTextSize(context.getResources().getDimension(R.dimen.heading_size_small_tablet) / scaledDensity);
            slideDesciption.setTextSize(context.getResources().getDimension(R.dimen.text_size_small_tablet) / scaledDensity);

        }

        slideHeading.setText(headings[position]);
        slideDesciption.setText(description[position]);



        container.addView(view);
        return view;
    }



    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

}
