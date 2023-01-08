package se.kth.iv1351.soundgoodschool.view;

import se.kth.iv1351.soundgoodschool.controller.Controller;
import se.kth.iv1351.soundgoodschool.model.InstrumentForRentDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BlockingInterpreter {
    private static final String PROMPT = "> ";
    private final Scanner console = new Scanner(System.in);
    private Controller ctrl;
    private boolean keepReceivingCmds = false;

    /**
     * Creates a new instance that will use the specified controller for all operations.
     *
     * @param ctrl The controller used by this instance.
     */
    public BlockingInterpreter(Controller ctrl) {
        this.ctrl = ctrl;
    }

    /**
     * Stops the command interpreter.
     */
    public void stop() {
        keepReceivingCmds = false;
    }

    /**
     * Interprets and performs user commands. This method will not return until the
     * UI has been stopped. The UI is stopped either when the user gives the
     * "quit" command, or when the method <code>stop()</code> is called.
     */
    public void handleCmds() {
        keepReceivingCmds = true;
        while (keepReceivingCmds) {
            try {
                CmdLine cmdLine = new CmdLine(readNextLine());
                switch (cmdLine.getCmd()) {
                    case HELP:
                        for (Command command : Command.values()) {
                            if (command == Command.ILLEGAL_COMMAND) {
                                continue;
                            }
                            System.out.println(command.toString().toLowerCase());
                        }
                        break;
                    case QUIT:
                        keepReceivingCmds = false;
                        break;
                    case LIST:
                        List<? extends InstrumentForRentDTO> instruments = ctrl.listRentalInstruments(cmdLine.getParameter(0));
                        enumerateInstruments(instruments);
                        break;
                    case RENT:
                        boolean rentalSuccessful = ctrl.rentInstrument(
                            cmdLine.getParameter(0),
                            cmdLine.getParameter(1),
                            cmdLine.getParameter(2),
                            cmdLine.getParameter(3)
                        );
                        if(rentalSuccessful) System.out.println("Rental successful");
                        break;
                    case TERMINATE:
                        boolean terminationSuccessful = ctrl.terminateRental(
                            cmdLine.getParameter(0),
                            cmdLine.getParameter(1),
                            cmdLine.getParameter(2)
                        );
                        if(terminationSuccessful) System.out.println("Termination successful");
                        break;
                    default:
                        System.out.println("Illegal command");
                }
            } catch (Exception e) {
                System.out.println("Operation failed");
                System.out.println(e.getMessage());
            }
        }
    }

    private String readNextLine() {
        System.out.print(PROMPT);
        return console.nextLine();
    }

    private void enumerateInstruments(List<? extends InstrumentForRentDTO> instruments) {
        for(InstrumentForRentDTO instrument : instruments) System.out.println(instrument);
    }
}