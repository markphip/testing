AJS.$(function ()
{
    require([
        'jira/admin/application/defaults/api',
        'bitbucket-invite-message-panel-view'
    ], function (
            defaultsApi,
            Panel
    )
    {
        "use strict";
        // listen to dialog show event
        defaultsApi.on(defaultsApi.EVENT_ON_SHOW, function(dialogView){
            if (dialogView.$el.attr("id") !== 'app-role-defaults-dialog')
            {
                return;
            }
            new Panel({
                jiraSoftwareCheckboxSelector: '.application-picker-applications .checkbox input.application-jira-software',
                el: 'div#bitbucket-invite-message-panel'
            });
        });
    });
})
