package com.knowledge.assistant.dto;

public class SlackUpdateCommand {

    private String ticketNumber;
    private String field;  // ASSIGNEE | STATUS | TITLE | PRIORITY
    private String value;

    public SlackUpdateCommand() {}

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
