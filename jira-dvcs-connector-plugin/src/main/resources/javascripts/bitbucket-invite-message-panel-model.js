define('bitbucket-invite-message-panel-model', [
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
            this.jiraSoftwareCheckboxContainerId = options.jiraSoftwareCheckboxContainerId;
            this.listenTo(defaultsApi, defaultsApi.EVENT_ON_SHOW, this.onDialogShow);
        },

        onDialogShow: function(checkboxContainer)
        {
            if (checkboxContainer.$el.attr("id") !== this.jiraSoftwareCheckboxContainerId) return;
            var self = this;
            var $jiraSoftwareCheckbox = checkboxContainer.$el.find(this.jiraSoftwareCheckboxSelector);
            // listen to the state of checkbox and change the model based on the new state
            $jiraSoftwareCheckbox.on('click', function ()
            {
                self.set({checked: ($(this).attr("checked"))});
            });

            // get checkbox initial state
            this.set({checked: $jiraSoftwareCheckbox.attr("checked")}, {silent: true});
            this.trigger('change');
        }
    });
});
