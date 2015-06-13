AJS.$(function() {
    if(AJS.$('.checkbox.application-jira-software').is(':checked')) {
        AJS.$('#should-invite-user').attr('checked', true);
    }

    AJS.InlineDialog(AJS.$("#add-user-bitbucket-access-extension-panel a.more-teams"),
            "add-user-bitbucket-access-extension",
            function(content, trigger, showPopup){
                content.html(dvcs.connector.bitbucket.access.moreTeamsInlineDialogContent({
                    teams: WRM.data.claim('bitbucket-access-inline-dialog-content')
                }));

                showPopup();
                return false;
            }, {
                width: 250
            }
    );
});
