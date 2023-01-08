package se.kth.iv1351.soundgoodschool.view;

public enum Command {
    /**
     * Lists all rented instruments.
     */
    LIST,
    /**
     * Sets the specified instrument as rented to the specified student.
     */
    RENT,
    /**
     * Terminates a rental.
     */
    TERMINATE,
    /**
     * Lists all commands.
     */
    HELP,
    /**
     * Exit the application.
     */
    QUIT,
    /**
     * None of the valid commands above was specified.
     */
    ILLEGAL_COMMAND
}
