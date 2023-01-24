from ctakes_pbj.type_system import ctakes_types


class EventCreator:

    def __init__(self, cas):
        self.event_mention_type = cas.typesystem.get_type
        self.event_type = cas.typesystem.get_type(ctakes_types.Event)
        self.event_properties_type = cas.typesystem.get_type(ctakes_types.EventProperties)

    def create_event(self, cas, dtr):
        e_props = self.event_properties_type
        e_props.docTimeRel = dtr
        cas.add(e_props)
        event = self.event_type
        event.properties = e_props
        cas.add(event)
        return event

    def create_event_mention(self, cas, dtr):
        event = self.create_event(cas, dtr)
        event_mention = self.event_mention_type
        event_mention.event = event
        cas.add(event_mention)
        return event_mention

