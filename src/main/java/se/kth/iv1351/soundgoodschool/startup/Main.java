package se.kth.iv1351.soundgoodschool.startup;

import se.kth.iv1351.soundgoodschool.controller.Controller;
import se.kth.iv1351.soundgoodschool.integration.SoundGoodDBException;
import se.kth.iv1351.soundgoodschool.view.BlockingInterpreter;

public class Main {
    /**
     * @param args There are no command line arguments.
     */
    public static void main(String[] args) {
        try {
            new BlockingInterpreter(new Controller()).handleCmds();
        } catch (Exception sgdbe) {
            System.out.println("Could not connect to SoundGood db.");
            sgdbe.printStackTrace();
        }
    }
}