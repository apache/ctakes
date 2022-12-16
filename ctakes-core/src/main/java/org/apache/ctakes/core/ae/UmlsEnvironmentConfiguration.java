package org.apache.ctakes.core.ae;


public enum UmlsEnvironmentConfiguration {
	URL {
		@Override
		public String toString() {
			return "ctakes.umlsaddr";
		}
	},
	VENDOR {
		@Override
		public String toString() {
			return "ctakes.umlsvendor";
		}
	},
	USER {
		@Override
		public String toString() {
			return "ctakes.umlsuser";
		}
	},
	PASSWORD {
		@Override
		public String toString() {
			return "ctakes.umlspw";
		}
	}
}
