define('application-access-bitbucket-access-extension-panel', [
    'jquery',
    'backbone'
], function ($,
        Backbone
)
{
    return Backbone.View.extend({
        initialize: function (options)
        {
            this.panelSelector = options.panelSelector;
            this.jiraSoftwareCheckboxSelector = options.jiraSoftwareCheckboxSelector;
            this.listenTo(options.defaultsApi, options.defaultsApi.EVENT_ON_SHOW, this.init);
        },

        changeVisibility: function (jiraSoftwareCheckboxChecked)
        {
            this.$el = $(this.panelSelector);
            jiraSoftwareCheckboxChecked ? this.$el.show() : this.$el.hide();
        },

        init: function (dialogView)
        {
            var self = this;
            $(dialogView.el).on('click', this.jiraSoftwareCheckboxSelector, function ()
            {
                self.changeVisibility($(this).attr("checked"));
            });
            var models = dialogView.collection.models;
            for (var index in models)
            {
                var model = models[index];
                var application = model.toJSON();
                if (application.key === "jira-software")
                {
                    this.changeVisibility(application.selectedByDefault);
                }
            }
        }
    })
});
