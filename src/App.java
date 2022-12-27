import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class App extends JFrame{
    private Map<String, String[]> courseData;
    private Map<String, String[]> weirdCourseData;
    private JPanel mainPane;
    private JLabel northLabel;
    private JLabel southLabel;
    private JTextField mainTextField;
    private JButton calculateButton;
    private boolean calculateClicked = false;
    private boolean calculateIsClicked = false;

    public App() throws FileNotFoundException {
        courseData = getParsedData(new Scanner(new File("data/courseData.txt")))[0];
        weirdCourseData = getParsedData(new Scanner(new File("data/courseData.txt")))[1];
        mainPane = new JPanel(new FlowLayout());
        northLabel = new JLabel("Type your requirements (separated by a slash) and press 'Calculate'.");
        southLabel = new JLabel();
        mainTextField = new JTextField("eg: MHF4U/MCV4U/ENG4U", 35);
        calculateButton = new JButton("Calculate");
        AbstractAction action1 = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {  
                if (calculateClicked) {
                    calculateIsClicked = true;
                } else {
                    calculateClicked = true;
                    southLabel.setText("");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            southLabel.setText(String.valueOf(getAverage(mainTextField.getText().split("/"), courseData, weirdCourseData)));

                        }
                    }).start();
                }
                repaint();  
            } 
        };
        calculateButton.addActionListener(action1);
        northLabel.setForeground(Color.CYAN);
        mainTextField.setForeground(Color.CYAN);
        mainTextField.setBackground(Color.DARK_GRAY.darker());
        mainTextField.setCaretColor(Color.CYAN);
        mainTextField.setBorder(null);
        calculateButton.setBackground(Color.DARK_GRAY.darker());
        calculateButton.setForeground(Color.CYAN);
        southLabel.setForeground(Color.CYAN);
        mainPane.add(northLabel);
        mainPane.add(mainTextField);
        mainPane.add(calculateButton);
        mainPane.add(southLabel);
        mainPane.setBackground(Color.DARK_GRAY);
        add(mainPane);
        setTitle("Ontario University Top 6 Calculator");
        setSize(650, 120);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        //System.out.println(getAverage(new String[] {"MHF4U", "MCV4U", "HET4U", "SPH4U", "SBI4U", "NHD4U"}, courseData, weirdCourseData));
    }

    // Parse the txt file
    private Map<String, String[]>[] getParsedData(Scanner s) {
        Map<String, String[]> parsedData = new HashMap<>();
        Map<String, String[]> weirdData = new HashMap<>();
        while (s.hasNext()) {
            String line = s.nextLine();
            String[] courses = line.split(":");
            if (courses.length == 2) {
                if (courses[1].startsWith("(")) {
                    weirdData.put(courses[0], courses[1].substring(1, courses[1].length()-2).split(","));
                } else {
                    parsedData.put(courses[0], courses[1].split("/"));
                }
            }
        }
        s.close();
        return new Map[] {parsedData, weirdData};
    }

    private double getAverage(String[] requirements, Map<String, String[]> data, Map<String, String[]> weirdData) {
        mainTextField.setText("");
        repaint();
        int[] averages = new int[6];

        for (int i = 0; i < 6; i++) {
            if (i >= requirements.length) {
                northLabel.setText("Enter your highest grade from any 12U (if none, 11U) course which you have not already entered: ");
                averages[i] = recursiveGradeRequest(new String[] {"NONE"}, data, weirdData);    
            } else {
                northLabel.setText("Enter your highest grade from this course: ");
                averages[i] = recursiveGradeRequest(new String[] {requirements[i]}, data, weirdData);    
            }
        }

        // Add all averages together, divide by 6
        double avg = 0;
        for (int i = 0; i < averages.length; i++) {
            avg += averages[i];
        }
        avg /= 6;
        return Math.round(avg);
    }

    private int recursiveGradeRequest(String courses[], Map<String, String[]> data, Map<String, String[]> weirdData) {
        if (!courses[0].equals("NONE")) {
            northLabel.setText(northLabel.getText() + Arrays.toString(courses));
            southLabel.setText("(enter -1 if you do not have a grade for any course mentioned above)");
        }
        while (!calculateIsClicked) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int grade = 0;
        try {
            grade = Integer.valueOf(mainTextField.getText());
        } catch (NumberFormatException e) {
            new ErrorOccured("Incorrect value entered.");
            dispose();
        } catch (Exception e1) {
            new ErrorOccured(e1.toString());
        }
        mainTextField.setText("");
        repaint();
        calculateIsClicked = false;
        // Check if user has a grade for this course
        if (grade <= -1) {
            // If not, check prerequisites
            int bestGrade = 0;
            for (String course : courses) {
                if (course.equals("NONE")) {
                    new ErrorOccured("Unexpected negative grade");
                    dispose();
                }
                String[] prerequisites;
                // If the course is in the database
                if (data.keySet().contains(course)) {
                    prerequisites = data.get(course);
                    if (prerequisites[0].equals("NONE")) {
                        northLabel.setText("Enter your highest 12U (if none, 11U) grade which you have not already entered: ");
                    } else {
                        northLabel.setText("Enter your highest grade from these courses: ");
                    }
                // If the course is in the weird database
                } else if (weirdData.keySet().contains(course.substring(0, 1))) {
                    prerequisites = weirdData.get(course.substring(0, 1));
                    if (prerequisites[0].equals("NONE")) {
                        northLabel.setText("Enter your highest 12U (if none, 11U) grade which you have not already entered: ");
                    } else {
                        northLabel.setText("Enter your highest grade from any 12U (if none, 11U) course starting with any of these letters: ");
                    }
                // If the course follows the default prerequisite format (eg: SCH4U -> SCH3U)
                } else {
                    int level;
                    if (course.length() == 1) {
                        level = 0;
                    } else {
                        level = Integer.valueOf(course.substring(3, 4))-1;
                    }
                    if (level < 3) {
                        prerequisites = new String[] {"NONE"};
                        northLabel.setText("Enter your highest 12U (if none, 11U) grade which you have not already entered: ");
                    } else {
                        prerequisites = new String[] {course.substring(0, 3) + String.valueOf(level) + course.charAt(4)};
                        northLabel.setText("Enter your highest grade from this course: ");
                    }
                }
                int evaluation = (recursiveGradeRequest(prerequisites, data, weirdData));
                if (evaluation > bestGrade) {
                    bestGrade = evaluation;
                }
            }
            return(bestGrade);
        } else {
            return(grade);
        }
    }

    private class ErrorOccured extends JFrame {
        private JPanel panel;
        private JLabel errorLabel;
        private ErrorOccured(String error) {
            panel = new JPanel(new FlowLayout());
            errorLabel = new JLabel("An error has occured: " + error);
            panel.add(errorLabel);
            add(panel);
            setSize(600, 100);
            setVisible(true);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    public static void main(String[] args) throws Exception {
        new App();
    }
}