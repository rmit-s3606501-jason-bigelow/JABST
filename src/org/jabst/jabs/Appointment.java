package org.jabst.jabs;

import org.jabst.jabs.util.*;
import java.util.Date;
import java.time.Duration;

/** Holds information about an appointment */
public class Appointment {
	private Date dateAndTime;
	/** This should refer to a valid type ID from the database */
	int appointmentType;// that enum tho
	/** Should refer to a valid employee ID from the database */
	private long employeeID;
	/** Customer object */
	private Customer customer;

	public Appointment (Date dateAndTime, int appointmentType,
		long employeeID, Customer customer)
	{
		this.dateAndTime = dateAndTime;
		this.appointmentType = appointmentType;
		this.employeeID = employeeID;
		this.customer = customer;

	}

	public Appointment (WeekDate weekDate, int appointmentType,
		long employeeID, Customer customer)
	{
		this(DayOfWeekConversion.wd2cal(weekDate).getTime(), appointmentType, employeeID, customer);
	}

	public Date getDate() {
		return dateAndTime;
	}

	public Duration getDuration() {
		// Ask the appointment type for the duration
		return AppointmentType.getByID(appointmentType).getDuration();
	}

	public int getAppointmentType() {
		return appointmentType;
	}

	public long getEmployeeID() {
		return employeeID;
	}

	public Customer getCustomer() {
		return customer;
	}

}