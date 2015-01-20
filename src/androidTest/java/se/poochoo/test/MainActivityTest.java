package se.poochoo.test;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.EditText;
import android.widget.SearchView;

import se.poochoo.MainActivity;
import se.poochoo.R;
import se.poochoo.proto.Messages.SmartListData;
import se.poochoo.proto.Messages.SmartResponse;
import se.poochoo.test.util.MockNetworkInterface;
import se.poochoo.test.util.ProtoUtil;

import com.robotium.solo.Solo;


import static android.test.ViewAsserts.assertOnScreen;

/**
 * Created by Erik on 2013-10-05.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        MockNetworkInterface.useRealInterface();
    }

    public void testDialogPopup() {
        // Mock the server response before creating the activity.
        MockNetworkInterface.useMockInterface();
        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());
        // Create the activity and give it some time to load.
        solo = new Solo(getInstrumentation(), getActivity());
        solo.sleep(2000);
        solo.searchText("11 Testland");
        solo.searchText("12 Testland");

        solo.searchText("1 min");
        solo.searchText("3 min");

        solo.searchText("Test Message in mainview 1");
        solo.searchText("Test Message in mainview 2");

        // The dialog will come up and load additional data, make sure we return some.
        MockNetworkInterface.addMockResponse(SmartResponse.newBuilder()
                .addListData(ProtoUtil.listItemWithName("11 Testland", "11 min", "Test Message in dialog 1", 100))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "12 min", "Test Message in dialog 2", 100))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "13 min", "Test Message in dialog 3", 100))
                .build()); // Three second delay to load data.

        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());
        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());

        solo.clickOnText("11 Testland");

        // Wait for dialog to open.
        solo.waitForActivity(se.poochoo.DialogActivity.class);
        solo.searchText("13 min");
        solo.searchText("13 min");
        solo.searchText("11 Testland");
        solo.searchText("Test Message in dialog 1");
        solo.searchText("Test Message in dialog 2");
        solo.searchText("Test Message in dialog 3");


        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());
        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());
        MockNetworkInterface.addMockResponse(ProtoUtil.buildInitialServerResponse());

        // Star mark the departure.
        solo.clickOnActionBarItem(R.id.action_star);

        // Click OK, and we're finished.
        solo.goBack();
        solo.sleep(3000);
        // We should now see the star.
        View starView = solo.getView(R.id.stardeparture);
        assertTrue(starView.isShown());

        //Start taxitest
        solo.scrollListToBottom(0);
        solo.clickOnText(getActivity().getString(R.string.cabCardText));
        solo.sleep(1000);
        solo.searchText(getActivity().getString(R.string.cabCardTextConfirm));
        solo.clickOnText(getActivity().getString(R.string.no));
        //End taxitest
    }

    /*
    public void testSwipe() {
        // Mock the server response before creating the activity.
        MockNetworkInterface.useMockInterface();
        MockNetworkInterface.addMockResponse(SmartResponse.newBuilder()
                .addListData(ProtoUtil.listItemWithName("11 Testland", "11 min", "Test Message in mainview 1"))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "12 min", "Test Message in mainview 2"))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "13 min", "Test Message in mainview 3"))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "15 min", "Test Message in mainview 4"))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "16 min", "Test Message in mainview 5"))
                .addListData(ProtoUtil.listItemWithName("11 Testland", "17 min", "Test Message in mainview 6"))
                .build());
        // Create the activity and give it some time to load.
        solo = new Solo(getInstrumentation(), getActivity());
        solo.searchText("11 Testland");

        solo.searchText("11 min");
        solo.searchText("12 min");
        solo.searchText("13 min");
        solo.searchText("14 min");
        solo.searchText("15 min");
        solo.searchText("16 min");

        solo.searchText("Test Message in mainview 1");
        solo.searchText("Test Message in mainview 2");
        solo.searchText("Test Message in mainview 3");
        solo.searchText("Test Message in mainview 4");
        solo.searchText("Test Message in mainview 5");
        solo.searchText("Test Message in mainview 6");
        solo.sleep(2000);
        int screenWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getActivity().getWindowManager().getDefaultDisplay().getHeight();
        int offset = 20;
        int y = screenHeight / 2;
        // Doesn't work :(
        // for (int i = 0; i < 5; i++) {
          // solo.drag(offset, screenWidth-offset, i*10 + y, i*10 + y,  screenWidth-offset);
        // }
    } */
}
