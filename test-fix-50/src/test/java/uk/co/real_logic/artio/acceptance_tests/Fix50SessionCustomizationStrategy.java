package uk.co.real_logic.artio.acceptance_tests;

import uk.co.real_logic.artio.builder.AbstractLogonEncoder;
import uk.co.real_logic.artio.builder.AbstractLogoutEncoder;
import uk.co.real_logic.artio.builder.LogonEncoder;
import uk.co.real_logic.artio.session.SessionCustomisationStrategy;

class Fix50SessionCustomizationStrategy implements SessionCustomisationStrategy
{
    public void configureLogon(final AbstractLogonEncoder logon, final long sessionId)
    {
        ((LogonEncoder)logon).defaultApplVerID("7");
    }

    public void configureLogout(final AbstractLogoutEncoder logout, final long sessionId)
    {
    }
}
