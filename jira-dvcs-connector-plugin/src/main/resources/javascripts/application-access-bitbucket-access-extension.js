AJS.$(function ()
{
    require([
        'jquery',
        'jira/admin/application/defaults/api',
        'application-access-bitbucket-access-extension-panel-model',
        'application-access-bitbucket-access-extension-panel'
    ], function (
            $,
            defaultsApi,
            PanelModel,
            Panel
    )
    {
        var panelModel = new PanelModel({
            jiraSoftwareCheckboxSelector: '.application-picker-applications .checkbox input.application-jira-software'
        });

        var panel = new Panel({
            model: panelModel
        });

    });
})
