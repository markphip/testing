AJS.$(function () {
    require([
        'bitbucket-invite-message-panel-view'
    ], function (
            Panel
    )
    {
        "use strict";

        new Panel({
            jiraSoftwareCheckboxSelector: '.application-picker-applications .checkbox input.application-jira-software',
            el: 'div#bitbucket-invite-message-panel'
        });

    });
})
