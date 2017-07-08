package com.sourcey.linachatbot;

/**
 * Created by amrezzat on 6/16/2017.
 */

public class characterNumber {
    public static int get(String character) {
        switch (character) {
            case "Casual":
                return 0;
            case "Adventurous":
                return 1;
            case "Friendly":
                return 2;
            case "Amusing":
                return 3;
            case "Devious":
                return 4;
            case "Urbane":
                return 5;
            case "Fanciful":
                return 6;
            case "Grieving":
                return 7;
            case "Frightened":
                return 8;
            case "Romantic":
                return 9;
            case "Imaginative":
                return 10;
            case "Gloomy":
                return 11;
        }
        return 0;
    }

    public static int getID(int character) {
        switch (character) {
            case 0:
                return R.id.ic_one;
            case 1:
                return R.id.ic_two;
            case 2:
                return R.id.ic_three;
            case 3:
                return R.id.ic_four;
            case 4:
                return R.id.ic_five;
            case 5:
                return R.id.ic_six;
            case 6:
                return R.id.ic_seven;
            case 7:
                return R.id.ic_eight;
            case 8:
                return R.id.ic_nine;
            case 9:
                return R.id.ic_ten;
            case 10:
                return R.id.ic_eleven;
            case 11:
                return R.id.ic_twelve;
        }
        return R.id.ic_one;
    }
    public static int getImg(int character){
        switch (character) {
            case 0:
                return R.drawable.casual;
            case 1:
                return R.drawable.adventurous;
            case 2:
                return R.drawable.friendly;
            case 3:
                return R.drawable.amusing;
            case 4:
                return R.drawable.devious;
            case 5:
                return R.drawable.urbane;
            case 6:
                return R.drawable.fanciful;
            case 7:
                return R.drawable.grieving;
            case 8:
                return R.drawable.frightened;
            case 9:
                return R.drawable.romantic;
            case 10:
                return R.drawable.imaginative;
            case 11:
                return R.drawable.gloomy;
        }
        return R.id.ic_one;
    }
}
