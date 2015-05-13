package ab.demo;

import ab.vision.ABType;

import java.awt.*;

/**
 * Simple class to store shot details. {@link #toString()} is overridden
 * to make it easier to write out a line to a file, avoiding any typos which
 * may occur.
 */
class CustomShot {

    /**
     * The number of the shot being taken.
     */
    private final int shotNum;

    /**
     * The release point.
     */
    private final Point point;

    /**
     * The type of the bird, as a String.
     */
    private final String bird;

    /**
     * The rating of the shot, as a String.
     */
    private final String rating;

    /**
     * Default constructor for CustomShot. Takes several arguments
     * and sets them to the class. The ABType passed in for the `bird`
     * parameter is converted to a String.
     *
     * @param bird the type of bird being fired.
     * @param point the release point.
     * @param shotNum the number of the shot.
     * @param rating the rating given to the shot.
     */
    public CustomShot(Point point, ABType bird, int shotNum, String rating){
        this.bird = bird + "";
        this.point = point;
        this.rating = rating;
        this.shotNum = shotNum;
    }

    /**
     * Override of the default ${@link Object#toString()} in order to
     * make writing a shot to a file easier (in .arff format).
     *
     * @return a ${@link java.lang.String} instance.
     */
    @Override
    public String toString(){
        return  // the x point of the release
                Math.abs(point.x) + "," +
                // the angle in degrees, with decimals trimmed
                Math.abs(point.y) + "," +
                // the type of bird being fired
                bird + "," +
                // the grouping of the shot
                ((int) Math.floor(shotNum / 2)) + "," +
                // the shot rating
                rating;
    }

}
