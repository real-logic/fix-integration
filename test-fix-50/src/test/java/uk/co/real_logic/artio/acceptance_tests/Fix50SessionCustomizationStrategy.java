package uk.co.real_logic.artio.acceptance_tests;

import uk.co.real_logic.artio.builder.LogonEncoder;
import uk.co.real_logic.artio.builder.LogoutEncoder;
import uk.co.real_logic.artio.session.SessionCustomisationStrategy;

class Fix50SessionCustomizationStrategy implements SessionCustomisationStrategy
{
    public void configureLogon(final LogonEncoder logon, final long sessionId)
    {
        logon.defaultApplVerID("7");
    }

    public void configureLogout(final LogoutEncoder logout, final long sessionId)
    {
    }
}
