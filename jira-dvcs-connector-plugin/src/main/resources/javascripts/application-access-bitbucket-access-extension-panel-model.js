define('application-access-bitbucket-access-extension-panel-model', [
    'jquery',
    'backbone',
    'jira/admin/application/defaults/api',
], function (
        $,
        Backbone,
        defaultsApi
)
{
    "use strict";
    // Model represent the state of the jira-software-checkbox
    return Backbone.Model.extend({

        initialize: function(options)
        {
            this.jiraSoftwareCheckboxSelector = options.jiraSoftwareCheckboxSelector;
            this.listenTo(defaultsApi, defaultsApi.EVENT_ON_SHOW, this.onDialogShow);
        },

        onDialogShow: function(dialogView)
        {
            if (dialogView.$el.attr("id") !== 'app-role-defaults-dialog') return;
            var self = this;

            // listen to the state of checkbox and change the model based on the new state
            dialogView.$el.on('click', this.jiraSoftwareCheckboxSelector, function ()
            {
                self.set({checked: ($(this).attr("checked"))});
            });

            // get checkbox initial state
            // since the web-panels is rendered before the checkbox, so we can get the checkbox initial state from the
            // backbone collection attached with the dialog view
            var models = dialogView.collection.models;
            for (var index in models)
            {
                var model = models[index];
                var application = model.toJSON();
                if (application.key === "jira-software")
                {
                    // since the panel is rendered every time the dialog is shown we can not save its previous state
                    // so instead of firing the change event on a "change" we fire it on every set
                    // so if previous value = new value the event still fired and the panel got the correct state
                    this.set({checked: application.selectedByDefault}, {silent: true});
                    this.trigger('change');
                    return;
                }
            }
        }
    });
});
