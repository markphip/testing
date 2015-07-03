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

        initialize: function (options)
        {
            this.el = options.el;
            this.model = options.model;
            this.model.on('change',this.changeVisibility.bind(this));
        },

        element: function()
        {
            return $(this.el);
        },

        changeVisibility: function ()
        {
            this.model.get("checked") ? this.element().show() : this.element().hide();
        }
    })
});
