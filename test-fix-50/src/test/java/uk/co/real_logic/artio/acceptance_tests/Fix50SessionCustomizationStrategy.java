package uk.co.real_logic.artio.acceptance_tests;

import uk.co.real_logic.artio.builder.LogonEncoder;
import uk.co.real_logic.artio.builder.LogoutEncoder;
import uk.co.real_logic.artio.session.SessionCustomisationStrategy;

class Fix50SessionCustomizationStrategy implements SessionCustomisationStrategy
{
    @Override
    public void configureLogon(LogonEncoder logon, long sessionId) {
        logon.defaultApplVerID("7");
    }

    @Override
    public void configureLogout(LogoutEncoder logout, long sessionId) {

    }
}
