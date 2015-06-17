define("dvcs/bitbucket-access", function () {
    "use strict";

    var BitbucketAccess = function($bitbucketAccessOption, $bitbucketInfoIcon) {
        this.$bitbucketAccessOption = $bitbucketAccessOption;
        this.$bitbucketInfoIcon = $bitbucketInfoIcon;
    };

    BitbucketAccess.prototype = {
        enable: function() {
            this.$bitbucketAccessOption.attr('disabled', false);
            this.$bitbucketInfoIcon.addClass('hidden');
        },

        disable: function() {
            this.deselect();
            this.$bitbucketAccessOption.attr('disabled', true);
            this.$bitbucketInfoIcon.removeClass('hidden');
        },

        select: function() {
            this.$bitbucketAccessOption.attr('checked', true);
        },

        deselect: function() {
            this.$bitbucketAccessOption.attr('checked', false);
        },

        isEnabled: function() {
            return !this.$bitbucketAccessOption.attr('disabled');
        },

        isSelected: function() {
            return this.$bitbucketAccessOption.attr('checked');
        }
    };

    return BitbucketAccess;
});
