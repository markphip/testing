define('bitbucket-invite-message-panel-view', [
    'jquery',
    'backbone',
    'aui/inline-dialog2'
], function (
        $,
        Backbone,
        InlineDialog2
)
{
    "use strict";
    // View bounded to the existing bitbucket access extension panel
    // (i.e. the view does not render the panel just bounded to it)
    return Backbone.View.extend({

        initialize: function (options)
        {
            this.el = options.el;
            this.model = options.model;
            this.model.on('change',this.changeVisibility.bind(this));
        },

        // function to always get a fresh instance of jQuery object
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
