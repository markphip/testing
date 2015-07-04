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
            this.jiraSoftwareCheckboxSelector = options.jiraSoftwareCheckboxSelector;
            this.$jiraSoftwareCheckbox = $(this.jiraSoftwareCheckboxSelector);
            this.$jiraSoftwareCheckbox.on("change", this.changeVisibility.bind(this));
            this.changeVisibility();
        },

        changeVisibility: function ()
        {
            this.$jiraSoftwareCheckbox.is(':checked') ? this.$el.show() : this.$el.hide();
        }
    })
});
