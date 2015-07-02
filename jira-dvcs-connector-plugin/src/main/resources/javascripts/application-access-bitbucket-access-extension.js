AJS.$(function ()
{
    require([
        'jquery',
        'jira/admin/application/defaults/api',
        'application-access-bitbucket-access-extension-panel',
        'aui/inline-dialog2'
    ], function (
            $,
            defaultsApi,
            Panel,
            InlineDialog2 //Unused. To use inline dialog 2, all I need to do is 'require' it.
    )
    {
        var panel = new Panel({
            panelSelector: 'div#application-access-bitbucket-access-extension-panel',
            jiraSoftwareCheckboxSelector: '.application-picker-applications .checkbox input.application-jira-software',
            defaultsApi: defaultsApi
        });
    });
})
