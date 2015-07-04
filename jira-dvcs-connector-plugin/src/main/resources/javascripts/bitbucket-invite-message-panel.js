AJS.$(function ()
{
    require([
        'jira/admin/application/defaults/api',
        'bitbucket-invite-message-panel-model',
        'bitbucket-invite-message-panel-view'
    ], function (
            defaultsApi,
            PanelModel,
            Panel
    )
    {
        "use strict";
        var panelModel = new PanelModel({
            jiraSoftwareCheckboxSelector: '.application-picker-applications .checkbox input.application-jira-software',
            jiraSoftwareCheckboxContainerId: 'app-role-defaults-dialog'
        });

        var panel = new Panel({
            model: panelModel,
            el: 'div#bitbucket-invite-message-panel'
        });

    });
})
