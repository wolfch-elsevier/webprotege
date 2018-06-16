package edu.stanford.bmir.protege.web.client.match;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DatePicker;
import edu.stanford.bmir.protege.web.resources.WebProtegeClientBundle;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 13 Jun 2018
 */
public class DateTimeViewImpl extends Composite implements DateTimeView {

    private static final WebProtegeClientBundle CLIENT_BUNDLE = GWT.create(WebProtegeClientBundle.class);

    private DatePicker datePicker;

    private PopupPanel popupPanel;

    interface DateTimeViewImplUiBinder extends UiBinder<HTMLPanel, DateTimeViewImpl> {

    }

    private static DateTimeViewImplUiBinder ourUiBinder = GWT.create(DateTimeViewImplUiBinder.class);

    @UiField
    TextBox dateField;

    private DateTimeFormat dateFormat = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_LONG);

    @Inject
    public DateTimeViewImpl() {
        initWidget(ourUiBinder.createAndBindUi(this));
        datePicker = new DatePicker();
        datePicker.addValueChangeHandler(this::handleDatePickerDateChanged);
        popupPanel = new PopupPanel(true, true);
        popupPanel.setWidget(datePicker);
    }

    @UiHandler("dateField")
    protected void handleDateFieldFocused(FocusEvent event) {
        showDatePicker();
    }

    @UiHandler("dateField")
    protected void handleDateFieldClicked(ClickEvent event) {
        showDatePicker();
    }

    protected void handleDatePickerDateChanged(ValueChangeEvent<Date> event) {
        popupPanel.setVisible(false);
        commitDate();
    }

    private void showDatePicker() {
        Date date = getEnteredDate();
        datePicker.setValue(date);
        datePicker.addStyleName(CLIENT_BUNDLE.dateTimePicker().picker());
        popupPanel.showRelativeTo(dateField);


    }

    private void commitDate() {
        Date selectedDate = datePicker.getValue();
        dateField.setValue(dateFormat.format(selectedDate));
    }

    private Date getEnteredDate() {
        Date date;
        try {
            date = dateFormat.parse(dateField.getText().trim());
        } catch (IllegalArgumentException e) {
            date = new Date();
        }
        return date;
    }

    @Override
    public int getYear() {
        return getEnteredDate().getYear() + 1900;
    }

    @Override
    public int getMonth() {
        return getEnteredDate().getMonth() + 1;
    }

    @Override
    public int getDay() {
        return getEnteredDate().getDate();
    }
}