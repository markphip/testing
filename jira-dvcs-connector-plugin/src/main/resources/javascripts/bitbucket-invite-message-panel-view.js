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
    // (i.e. the view does not render the panel)
    return Backbone.View.extend({

        inlineDialogIdConstantPart: "bitbucket-access-inline-dialog",

        initialize: function (options){
            this.el = options.el;

            this.$jiraSoftwareCheckbox = $(options.jiraSoftwareCheckboxSelector);
            this.$jiraSoftwareCheckbox.on("change", this.changeVisibility.bind(this));

            var $inlineDialog = this.$el.find(".aui-inline-dialog");
            var $inlineDialogLink = this.$el.find("#bitbucket-access-inline-dialog-link");

            this.changeVisibility();

            // why we need dynamic part of inline dialog Id
            // because when the panel is rendered inside a dialog every time the dialog is shown we render new panel
            // which means we render new inline-dialog so we need to re-adjust its position
            // while if we use same id every time, inline dialog position will not be adjusted and will be rendered at the top left
            var inlineDialogId = this.inlineDialogIdConstantPart + '-' + new Date().getTime();
            $inlineDialog.attr("id", inlineDialogId);
            $inlineDialogLink.attr("aria-controls", inlineDialogId);
        },

        changeVisibility: function (){
            this.$jiraSoftwareCheckbox.is(':checked') ? this.$el.show() : this.$el.hide();
        },

        close: function() {
            this.remove();
            this.unbind();
        }
    });
});
