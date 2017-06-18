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
            case "Amiable":
                return 2;
            case "Amusing":
                return 3;
            case "Devious":
                return 4;
            case "Debonair":
                return 5;
            case "Fanciful":
                return 6;
            case "Agonizing":
                return 7;
            case "Frightening":
                return 8;
            case "Romantic":
                return 9;
            case "Dreamy":
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
}
