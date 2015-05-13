package ab.demo;

import ab.demo.other.ActionRobot;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.utils.StateUtil;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A custom Agent, making use of Bayesian learning mechanisms
 * in order to play the game of Angry Birds.
 */
public class AssignmentAgent implements Runnable {

    private int currentLevel, rejected, shotsFired;

	private ActionRobot aRobot;
	private Map<Integer,Integer> scores = new LinkedHashMap<>();
    private Point prevTarget;
    private Random randomGenerator;
	private TrajectoryPlanner tp;

    /**
     * Main entry point for the Agent. Creates a temporary
     * directory if it does not already exist.
     *
     * @param args the program arguments
     */
    public static void main(String args[]) throws Exception {
        // create the temporary path as a String
        String tmpPath = "./" + AssignmentAgentUtils.TMP_DIRECTORY;
        // create a File from the String
        File tmpFile = new File(tmpPath);
        // check if path exists and is directory
        if(!tmpFile.exists() || !tmpFile.isDirectory()){
            // if not, create directory
            Files.createDirectory(Paths.get(tmpPath));
        }
        // start up a new AssignmentAgent starting at level 1, or as specified
        new AssignmentAgent(args.length > 0 ? Integer.parseInt(args[0]) : 35).run();
    }

    /**
     * Simple constructor to set up the desired level and
     * initialize various fields within the Agent itself.
     *
     * @param currentLevel the level to start playing on.
     */
	public AssignmentAgent(int currentLevel) {
        // set up a new ActionRobot instance
		this.aRobot = new ActionRobot();
        // the current level to play
        this.currentLevel = currentLevel;
        // initialize the previous target to null
        this.prevTarget = null;
        // create a new Random to generate random values
        this.randomGenerator = new Random();
        // set rejected counter to 0
        this.rejected = 0;
        // set shots counter to 0
        this.shotsFired = 0;
        // create a new TrajectoryPlanner
        this.tp = new TrajectoryPlanner();
        // go to the Poached Eggs episode page
		ActionRobot.GoFromMainMenuToLevelSelection();
	}

    /**
     * Override the ${@link Runnable#run()} to handle the Bayesian
     * learning mechanism in order to play Angry Birds. Calls the
     * custom ${@link #solve()} method in order to play the game.
     */
	@Override
	public void run() {
        // load the desired level
		aRobot.loadLevel(currentLevel);

        // run the Agent until manually cancelled
        // noinspection InfiniteLoopStatement
        while (true) {
            // wrap in a try/catch in order to avoid crashes
            try {
                // run the solve method
                GameState state = solve();

                // track whether the level ended
                boolean isOver = true;

                // check the game state
                switch(state){
                    // if the level was won
                    case WON:
                        try {
                            // wait a couple of seconds
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        // get the score at the end of the level
                        int score = StateUtil.getScore(ActionRobot.proxy);
                        // if there is no score stored for this level
                        if (!scores.containsKey(currentLevel)) {
                            // add it to the stores
                            scores.put(currentLevel, score);
                        } else {
                            // if the existing score is lower
                            if (scores.get(currentLevel) < score) {
                                // update it
                                scores.put(currentLevel, score);
                            }
                        }

                        // calculate the total scores
                        int totalScore = 0;
                        // for all keys in the scores map
                        for (Integer key : scores.keySet()) {
                            // sum up
                            totalScore += scores.get(key);
                            // print level-by-level
                            System.out.println(" Level " + key + " Score: " + scores.get(key) + " ");
                        }
                        // print the total score
                        System.out.println("Total Score: " + totalScore);

                        // write an empty line to the .arff, to see level-breaks
                        AssignmentAgentUtils.appendFile(AssignmentAgentUtils.createLevelFile(currentLevel), "");
                        // load the next level
                        aRobot.loadLevel(++currentLevel);
                        // make a new trajectory planner whenever a new level is entered
                        tp = new TrajectoryPlanner();
                        // done
                        break;

                    case LOST:
                        // print out a restart message
                        System.out.println("Restart");
                        // restart the current level
                        aRobot.restartLevel();
                        // write an empty line to the .arff, to see level-breaks
                        AssignmentAgentUtils.appendFile(AssignmentAgentUtils.createLevelFile(currentLevel), "");
                        // done
                        break;

                    case LEVEL_SELECTION:
                        // print out a warning
                        System.out.println("Unexpected level selection page, go to " +
                                "the last current level : " + currentLevel);
                        // attempt to load the current level
                        aRobot.loadLevel(currentLevel);
                        // done
                        break;

                    case MAIN_MENU:
                        // print out a warning
                        System.out.println("Unexpected main menu page, go to the " +
                                "last current level : " + currentLevel);
                        // go to the level selection screen
                        ActionRobot.GoFromMainMenuToLevelSelection();
                        // attempt to load the current level
                        aRobot.loadLevel(currentLevel);
                        // done
                        break;

                    case EPISODE_MENU:
                        // print out a warning
                        System.out.println("Unexpected episode menu page, go to the " +
                                "last current level : " + currentLevel);
                        // go to the level selection screen
                        ActionRobot.GoFromMainMenuToLevelSelection();
                        // attempt to load the current level
                        aRobot.loadLevel(currentLevel);
                        // done
                        break;

                    default:
                        isOver = false;
                        break;
                }

                // if ended
                if(isOver){
                    // reset rejected
                    rejected = 0;
                    // reset shot count
                    shotsFired = 0;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
		}
	}

    /**
     * Finds the distance between two points.
     *
     * @param p1 the first point.
     * @param p2 the second point.
     * @return the distance as a double.
     */
	private double distance(Point p1, Point p2) {
		return Math.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
	}

    /**
     * Solve the current shot using a Bayesian learning mechanism in order
     * to calculate good/bad shots.
     *
     * @return a ${@link GameState} instance.
     */
	public GameState solve() throws Exception {
		// process image
		Vision initialVision = new Vision(ActionRobot.doScreenShot());

		// find the slingshot
		Rectangle sling = initialVision.findSlingshotMBR();

        // bayes' prediction
        String prediction;

		// confirm the slingshot
		while (sling == null && aRobot.getState() == GameState.PLAYING) {
            // warning
			System.out.println("No slingshot detected. Please remove pop up or zoom out");
            // zoom out
			ActionRobot.fullyZoomOut();
            // create a Vision from the screenshot
            initialVision = new Vision(ActionRobot.doScreenShot());
            // find the new sling
			sling = initialVision.findSlingshotMBR();
		}

        // the initial blocks at the start of the shot
        List<ABObject> initialBlocks = initialVision.findBlocksRealShape();
        // the initial pigs at the start of the shot
 		List<ABObject> initialPigs = initialVision.findPigsRealShape();

        // get current state
		GameState state = aRobot.getState();

		// if there is a sling, then play, otherwise just skip.
		if (sling != null) {
            // check there are pigs
			if (!initialPigs.isEmpty()) {
                // the release point
				Point releasePoint = null;
                // the shot to take
				Shot shot;
                // release
				int dx, dy;

                // random pick up a pig
                ABObject pig = initialPigs.get(randomGenerator.nextInt(initialPigs.size()));

                // if the target is very close to before, randomly choose a point near it
                Point _tpt = pig.getCenter();

                // if the new target is close to the previous target
                if (prevTarget != null && distance(prevTarget, _tpt) < 10) {
                    // find a random angle
                    double _angle = randomGenerator.nextDouble() * Math.PI * 2;
                    // find a random release X
                    _tpt.x = _tpt.x + (int) (Math.cos(_angle) * 10);
                    // find a random release Y
                    _tpt.y = _tpt.y + (int) (Math.sin(_angle) * 10);
                    // log out the new point
                    System.out.println("Randomly changing to " + _tpt);
                }

                // sleep for a bit
                Thread.sleep(1000);

                // get the type of the bird on the sling
                ABType birdId = aRobot.getBirdTypeOnSling();

                // if it's unknown, restart due to error
                if(birdId == ABType.Unknown){
                    return state;
                }

                // estimate the trajectory
                ArrayList<Point> pts = tp.estimateLaunchPoint(sling, _tpt);

                // if we can do a high shot for a white bird
                if (pts.size() > 1 && birdId == ABType.WhiteBird) {
                    releasePoint = pts.get(1);
                } else if (pts.size() == 1) {
                    // we can only do the first shot
                    releasePoint = pts.get(0);
                } else if (pts.size() == 2) {
                    // randomly choose between the trajectories, with a 1 in
                    // 6 chance of choosing the high one
                    if (randomGenerator.nextInt(6) == 0) {
                        releasePoint = pts.get(1);
                    } else {
                        releasePoint = pts.get(0);
                    }
                } else {
                    // if there are no points
                    if (pts.isEmpty()) {
                        // log warning
                        System.out.println("No release point found for the target");
                        System.out.println("Try a shot with 45 degree");
                        // fire at 45˚
                        releasePoint = tp.findReleasePoint(sling, Math.PI / 4);
                    }
                }

                // get the reference point
                Point refPoint = tp.getReferencePoint(sling);

                // calculate the tapping time according the bird type
                if (releasePoint != null) {
                    // calculate the release angle
                    double releaseAngle = tp.getReleaseAngle(sling, releasePoint);
                    // print the angle
                    System.out.println("Release Point: " + releasePoint);
                    System.out.println("Release Angle: "
                            + Math.toDegrees(releaseAngle));

                    // calculate the tap time
                    int tapInterval;

                    // based on the bird id
                    switch (birdId) {

                        // tapping is redundant
                        case RedBird:
                            tapInterval = 0;
                            break;

                        // yellow birds need tapping
                        case YellowBird:
                            // if we're not a high shot
                            if(releaseAngle < 45) {
                                // tap 70-80% of the way
                                tapInterval = 70 + randomGenerator.nextInt(10);
                            } else {
                                // tap 80-90% of the way
                                tapInterval = 80 + randomGenerator.nextInt(10);
                            }
                            break;

                        // white birds bomb on tap
                        case WhiteBird:
                            tapInterval = 80 + randomGenerator.nextInt(15);
                            break; // 80-95% of the way

                        // black birds explode
                        case BlackBird:
                            // if we're not a high shot
                            if(releaseAngle < 40) {
                                // randomly after 90%
                                tapInterval = 90 + randomGenerator.nextInt(10);
                            } else {
                                // explode on contact
                                tapInterval = 100;
                            }
                            break; // 85-100% of the way

                        // blue birds split up
                        case BlueBird:
                            // tap 65-80% of the way
                            tapInterval = 65 + randomGenerator.nextInt(15);
                            break;

                        // default tap interval
                        default:
                            tapInterval = 60;
                    }

                    // calculate the tap time
                    int tapTime = tp.getTapTime(sling, releasePoint, _tpt, tapInterval);

                    // calculate release points
                    dx = (int) releasePoint.getX() - refPoint.x;
                    dy = (int) releasePoint.getY() - refPoint.y;

                    // create a new shot instance
                    shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);

                    // create a file instance for the current level
                    File levelFile = AssignmentAgentUtils.createLevelFile(currentLevel);

                    // create a file instance for the temporary level
                    File template = new File("./" + AssignmentAgentUtils.DATA_DIRECTORY + "/template.arff");

                    // if there is no existing file for this level
                    if(!levelFile.exists()){
                        // copy the template file to the level file
                        Files.copy(template.toPath(), levelFile.toPath());
                    }

                    // log out some info
                    System.out.println("Calculating shot quality...");

                    // create a temporary file for this level
                    File tmpLevel = AssignmentAgentUtils.createTempFile(currentLevel);

                    // if it exists before planning
                    if(tmpLevel.exists()){
                        // delete the file
                        Files.delete(tmpLevel.toPath());
                    }

                    // copy the level file to the temporary level
                    Files.copy(levelFile.toPath(), tmpLevel.toPath());

                    // append an unrated shot to the temporary level
                    AssignmentAgentUtils.appendFile(tmpLevel, new CustomShot(
                        new Point(dx, dy), birdId, shotsFired, "?"
                    ).toString());

                    // read in the temporary instances
                    ConverterUtils.DataSource source =new ConverterUtils.DataSource(
                            "./" + AssignmentAgentUtils.TMP_DIRECTORY + "/level" + currentLevel + ".arff"
                    );

                    // get the data from the source
                    Instances tmpInstances = source.getDataSet();

                    // ensure the class attribute is set
                    if(tmpInstances.classIndex() == -1){
                        tmpInstances.setClassIndex(tmpInstances.numAttributes() - 1);
                    }

                    // create an instance of the NaiveBayes classifier
                    NaiveBayes naiveBayes = new NaiveBayes();

                    // pass the temp instances to the classifier
                    naiveBayes.buildClassifier(tmpInstances);

                    // get the last instance (with the "?" rating)
                    Instance tmpInstance = tmpInstances.lastInstance();
                    tmpInstance.setClassValue(naiveBayes.classifyInstance(tmpInstance));

                    // retrieve the shot prediction
                    prediction = tmpInstance.stringValue(tmpInstance.numAttributes() - 1);

                    // delete the temporary file
                    if(!tmpLevel.delete()){
                        // warn if unable
                        System.err.println("Unable to remove temporary file!");
                    }

                    // if it's going to be a bad shot
                    if("bad".equals(prediction)){
                        // reject up to 3 shots, to try find a good shot
                        // only reject if we have a fair sample size to work with
                        if(tmpInstances.numInstances() + 1 > 5 && rejected++ < 3){
                            return state;
                        }

                        // if we have rejected at least 3 times
                        if(rejected >= 3){
                            // warning of rejection
                            System.out.println("Rejecting bad shot...");

                            // 50% chance of true
                            if(randomGenerator.nextBoolean()){
                                // warning
                                System.out.println("High arc towards pig...");

                                // calculate launch point towards a random pig
                                List<Point> launchPoints = tp.estimateLaunchPoint(
                                    sling, initialPigs.get(randomGenerator.nextInt(initialPigs.size())).getCenter()
                                );

                                // choose a high or low shot
                                if(launchPoints.size() > 1 && randomGenerator.nextBoolean()){
                                    releasePoint = launchPoints.get(1);
                                } else {
                                    releasePoint = launchPoints.get(0);
                                }
                            } else {
                                // warning
                                System.out.println("Random shot...");

                                // calculate a random angled shot
                                releasePoint = tp.findReleasePoint(sling, Math.toRadians(Math.random() * 80));
                            }
                            // calculate the tap time
                            tapTime = tp.getTapTime(sling, releasePoint, _tpt, tapInterval);

                            // calculate release points
                            dx = (int) releasePoint.getX() - refPoint.x;
                            dy = (int) releasePoint.getY() - refPoint.y;

                            // create a new shot instance
                            shot = new Shot(refPoint.x, refPoint.y, dx, dy, 0, tapTime);
                        }
                    }
                } else {
                    // warning
                    System.err.println("No Release Point Found");
                    // exit early
                    return state;
                }

                // store the previous shot
                prevTarget = new Point(_tpt.x, _tpt.y);

				// zoom out the screen
                ActionRobot.fullyZoomOut();

                // take a screenshot of the page
                Vision vision = new Vision(ActionRobot.doScreenShot());

                // find the slingshot rectangle
                Rectangle _sling = vision.findSlingshotMBR();

                // ensure we can see the sling
                if(_sling != null) {
                    // calculate scale difference
                    double scale_diff = Math.pow((sling.width - _sling.width), 2) + Math.pow((sling.height - _sling.height), 2);

                    // ensure scale has not changed too much
                    if (scale_diff < 25) {
                        if (dx < 0) {
                            // fire the shot
                            aRobot.cshoot(shot);

                            // get the state of the shot
                            state = aRobot.getState();

                            // if the game is still playing
                            if (state == GameState.PLAYING) {
                                // screenshot and adjust trajectory
                                vision = new Vision(ActionRobot.doScreenShot());
                                List<Point> traj = vision.findTrajPoints();
                                tp.adjustTrajectory(traj, sling, releasePoint);
                            }

                            // all shots are bad by default
                            String rating = "bad";

                            // find the initial counts of birds/blocks/pigs
                            int initialBirdSize = initialVision.findBirdsMBR().size();
                            int initialBlockSize = initialBlocks.size();
                            int initialPigSize = initialPigs.size();

                            // calculate the percentage of destruction
                            double destructionPercentage = Math.abs(100 -
                                    (((double) vision.findBlocksRealShape().size() / initialBlockSize) * 100));

                            // default to a single pig kill
                            double pigsNeeded = 1;

                            // if there are more starting pigs than birds
                            if(initialPigSize > initialBirdSize){
                                // scale appropriately
                                pigsNeeded = Math.floor(initialPigSize / initialBirdSize);
                                // ensure kill at least one big
                                if(pigsNeeded == 0){
                                    pigsNeeded = 1;
                                }
                            }

                            // calculate the number of pigs killed
                            int pigDeaths = Math.abs(initialPigSize - vision.findPigsRealShape().size());

                            // if we are over 15% destruction, this was a good shot OR
                            // if we killed a satisfactory amount of pigs, this was a good shot OR
                            // if the game is somehow won without this, this is a good shot.
                            if(destructionPercentage >= 15 || pigDeaths >= pigsNeeded || state == GameState.WON) {
                                rating = "good";
                            }

                            // log notifications of information
                            System.out.println("Was" + ("good".equals(rating) ? "" : " not") + " a good shot.");
                            System.out.println("Destruction: " + destructionPercentage + "%");
                            System.out.println("Pig Deaths: " + pigDeaths);

                            // create a shot instance
                            CustomShot customShot = new CustomShot(new Point(dx, dy), birdId, shotsFired, rating);

                            // increment the shot count
                            shotsFired += 1;

                            // append the file with the shot result
                            AssignmentAgentUtils.appendFile(AssignmentAgentUtils.createLevelFile(currentLevel), customShot.toString());
                        }
                    } else {
                        // warning
                        System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
                    }
                } else {
                    // warning
                    System.out.println("no sling detected, can not execute the shot, will re-segement the image");
                }
			}

		}
		return state;
	}
}
