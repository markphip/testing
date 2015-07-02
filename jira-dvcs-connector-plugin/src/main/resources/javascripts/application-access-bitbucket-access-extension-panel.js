define('application-access-bitbucket-access-extension-panel', [
    'jquery',
    'backbone',
    'aui/inline-dialog2'
], function (
        $,
        Backbone,
        InlineDialog2
)
{
    return Backbone.View.extend({

        el : 'div#application-access-bitbucket-access-extension-panel',

        initialize: function (options)
        {
            this.el = 'div#application-access-bitbucket-access-extension-panel';
            this.model = options.model;
            this.model.on('change',this.changeVisibility.bind(this));
        },

        element: function()
        {
            return $(this.el);
        },

        changeVisibility: function (model)
        {
            model.get("checked") ? this.element().show() : this.element().hide();
        }
    })
});
