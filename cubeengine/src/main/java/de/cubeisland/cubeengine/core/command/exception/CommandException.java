package de.cubeisland.cubeengine.core.command.exception;

/**
 * This exception will be catched by the executor.
 * Its message will be send to the command sender.
 */
public class CommandException extends RuntimeException
{
    public CommandException()
    {
        super();
    }

    public CommandException(String message)
    {
        super(message);
    }

    public CommandException(Throwable cause)
    {
        super(cause);
    }

    public CommandException(String message, Throwable cause)
    {
        super(message, cause);
    }
}