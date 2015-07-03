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
            dialogView.$el.on('click', this.jiraSoftwareCheckboxSelector, function ()
            {
                self.set({checked: ($(this).attr("checked"))});
            });
            // get checkbox initial state
            var models = dialogView.collection.models;
            for (var index in models)
            {
                var model = models[index];
                var application = model.toJSON();
                if (application.key === "jira-software")
                {
                    this.set({checked: application.selectedByDefault}, {silent: true});
                    this.trigger('change');
                    return;
                }
            }
        }
    });
});
