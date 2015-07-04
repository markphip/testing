AJS.test.require("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:bitbucket-invite-message-panel-component", function () {
    "use strict";

    require([
        'jquery',
        'bitbucket-invite-message-panel-view'
    ], function(
            $,
            PanelView
    ) {
        module('Application Role Defaults BB Panel', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                var PANEL_ID = "panelId";
                var JIRA_SOFTWARE_CHECKBOX_ID = 'checkboxId';

                $('#qunit-fixture').append($("<div id='" + PANEL_ID + "' />"));
                $('#qunit-fixture').append($("<input id='" + JIRA_SOFTWARE_CHECKBOX_ID + "' type='checkbox' />"));

                this.jiraSoftwareCheckboxSelector = "#" + JIRA_SOFTWARE_CHECKBOX_ID;
                this.$jiraSoftwareCheckbox = $(this.jiraSoftwareCheckboxSelector);

                this.panelSelector = "div#" + PANEL_ID;
                this.$panel = $(this.panelSelector);
            },

            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Panel shouldn't be visible when software checkbox isn't checked", function() {
            this.$jiraSoftwareCheckbox.prop("checked", false);
            new PanelView({
                jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector,
                el: this.panelSelector
            });

            equal(this.$panel.is(":visible"), false);
        });

        test("Panel should be visible when software checkbox is checked", function() {
            this.$jiraSoftwareCheckbox.prop("checked", true);
            new PanelView({
                jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector,
                el: this.panelSelector
            });

            equal(this.$panel.is(":visible"), true);
        });

        test("Panel should respond to software checkbox changes", function() {
            this.$jiraSoftwareCheckbox.prop("checked", false);
            new PanelView({
                jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector,
                el: this.panelSelector
            });

            equal(this.$panel.is(":visible"), false);

            this.$jiraSoftwareCheckbox.click();

            equal(this.$panel.is(":visible"), true);
        });
    })
});