AJS.$(function () {
    require([
        'jira/admin/application/defaults/api',
        'bitbucket-invite-message-panel-view'
    ], function (
        defaultsApi,
        Panel
    )
    {
        "use strict";

        defaultsApi.on(defaultsApi.EVENT_ON_SHOW, function(dialogView) {
            // listen only to the show event fired by the app role defaults dialog
            // which contains the bitbucket invite message panel
            if (dialogView.$el.attr("id") !== 'app-role-defaults-dialog'){
                return;
            }

            new Panel({
                jiraSoftwareCheckboxSelector: '.application-picker-applications .checkbox input.application-jira-software',
                el: 'div#bitbucket-invite-message-panel'
            });
        });
    });
})
